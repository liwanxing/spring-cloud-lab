package com.liwx.labgateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * B阶段：统一认证上移网关（胖网关模式）。
 * 所有外部流量在网关完成登录校验：token 有效则把 userId 塞进 X-User-Id 头下发，
 * 业务服务从"自己查会话"瘦成"信任网关头"（lab-common 的 @CurrentUserId 参数解析器取值）。
 *
 * 三段式：
 *  1. 内部专属接口（Feign 直连用的 deduct/restore）：外部一律 403 —— Feign 不走网关，不受影响
 *  2. 白名单：登录 + 商品 GET（未登录可浏览），其余全部要 token
 *  3. 剥掉外部伪造的 X-User-Id，注入网关验明的真实 userId
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 验明后注入的用户身份头（与 lab-common CurrentUserIdResolver 的常量约定一致；网关不依赖 lab-common，改一处记两处） */
    public static final String X_USER_ID = "X-User-Id";
    private static final String TOKEN_HEADER = "Authorization";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 白名单：无条件放行（登录接口本体） */
    private static final String[] WHITELIST = {"/api/auth/login"};
    /** 白名单（仅 GET）：商品浏览，写操作（增删改）仍需登录 */
    private static final String[] WHITELIST_GET = {"/api/products/**"};
    /** 内部专属：lab-order 经 Feign 直连调用（不经网关），外部到达即拒绝 —— 防绕过订单直接刷库存 */
    private static final String[] INTERNAL_ONLY = {"/api/products/*/deduct", "/api/products/*/restore"};

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (matchAny(INTERNAL_ONLY, path)) {
            return reject(exchange, HttpStatus.FORBIDDEN, 403, "无访问权限");
        }
        if (matchAny(WHITELIST, path) || (HttpMethod.GET.equals(method) && matchAny(WHITELIST_GET, path))) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        Object loginId = (token == null || token.isBlank()) ? null : StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            log.info("[网关鉴权] 拒绝 {} {}：token 缺失或已失效", method, path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
        }

        // 剥掉外部伪造的 X-User-Id，换成网关验明的真身 —— 下游只信这个头
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(X_USER_ID);
                    h.set(X_USER_ID, loginId.toString());
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean matchAny(String[] patterns, String path) {
        for (String p : patterns) {
            if (MATCHER.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    /** 统一 401/403 响应：与业务侧 GlobalExceptionHandler 的 Result 结构同构，前端拦截器无需区分来源 */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** 排在 TraceIdGlobalFilter（HIGHEST_PRECEDENCE）之后：先发 traceId 再鉴权，被拒的请求也能按 traceId 查到日志 */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
