package com.liwx.labcommon.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 阶段6（ELK 彩蛋）：traceId 全链路 —— HTTP 入口统一接管。
 * 收网关下发的 X-Trace-Id（没有则自生成，兼容直连服务的场景），塞进 MDC：
 * 控制台 pattern 的 %X{traceId} 与文件侧 LogstashEncoder（MDC 默认输出为 JSON 顶层字段）都会带上，
 * Kibana 按 traceId 字段一筛，网关->订单->商品的整条链路全出来。
 * 置于最前：保证后续所有 Controller/Service 日志都已带 traceId；
 * 请求结束 MDC.clear() 兜底清理（连同业务代码埋的 orderId 一起，线程池复用不串味）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 与 lab-gateway TraceIdGlobalFilter 约定的请求头名（网关不依赖本模块，常量各自维护、改一处记两处） */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    /** MDC 键：链路追踪 ID，随每条日志输出 */
    public static final String MDC_TRACE_ID = "traceId";
    /** MDC 键：业务埋点（下单/支付/关单等订单相关日志按它检索） */
    public static final String MDC_ORDER_ID = "orderId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);
        // 回显给调用方：线上报障时用户报一个响应头里的 traceId，Kibana 一查一个准
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
