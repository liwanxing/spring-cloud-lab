package com.liwx.labgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 阶段6（ELK 彩蛋）：traceId 全链路第一棒 —— 流量入口统一发号。
 * 请求头无 X-Trace-Id 则生成一个并改写请求传给下游路由；有则透传（外部已带就沿用）。
 * 下游服务的 TraceIdFilter（lab-common）从请求头接入 MDC，于是 网关->订单->商品 整条链路同一 traceId。
 * 注意：WebFlux 线程随时切换，MDC 不能跨订阅传播 —— 此处打日志前同步 put、打完即清，
 * 只保证本条入口日志的 JSON 里有 traceId 字段（与业务服务的字段检索体验对齐）。
 */
@Slf4j
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    /** 与 lab-common TraceIdFilter 约定的请求头名（网关不依赖 lab-common，常量各自维护、改一处记两处） */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            String tid = UUID.randomUUID().toString().replace("-", "");
            exchange = exchange.mutate()
                    .request(r -> r.headers(h -> h.set(TRACE_ID_HEADER, tid)))
                    .build();
            traceId = tid;
        }
        // 回显给调用方（curl -i 可见），并记一条入口日志 —— Kibana 链路的第一环
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        MDC.put(MDC_TRACE_ID, traceId);
        try {
            log.info("[网关] 转发请求 {}", exchange.getRequest().getPath().value());
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
