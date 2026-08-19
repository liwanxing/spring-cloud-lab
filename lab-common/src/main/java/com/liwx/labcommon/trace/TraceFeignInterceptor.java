package com.liwx.labcommon.trace;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 阶段6：traceId 跨服务透传 —— Feign 出站请求自动带上 MDC 里的 traceId，
 * 下游服务的 TraceIdFilter 收到后延续同一个，链路跨服务不断（lab-order -> lab-product）。
 * 与 Seata XID 的传播是同一个思路：上下文跟着请求头走。
 */
@Component
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
        if (traceId != null) {
            template.header(TraceIdFilter.TRACE_ID_HEADER, traceId);
        }
    }
}
