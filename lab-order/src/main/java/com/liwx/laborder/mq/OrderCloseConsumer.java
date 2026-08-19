package com.liwx.laborder.mq;

import com.liwx.labcommon.trace.TraceIdFilter;
import com.liwx.laborder.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 延迟关单消费者：下单时发出的延迟消息到点投递（延迟档位见 order.close-delay-level），
 * 触发关单检查。消息只带"提醒"职责，不带决定权：
 * 订单仍为 PENDING 才真正关单（渠道确认 -> 条件取消 -> 回补库存），
 * 已支付/已取消/不存在（全局事务回滚过）一律空跳 —— 天然幂等，重复投递无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqTopics.ORDER_CLOSE, consumerGroup = "lab-order-close-consumer")
public class OrderCloseConsumer implements RocketMQListener<MessageExt> {

    private final OrderService orderService;

    /**
     * 用 MessageExt 而非 String：需要消息属性（下单时塞的 traceId 头），String 只能拿到 body。
     */
    @Override
    public void onMessage(MessageExt message) {
        String orderId = new String(message.getBody(), StandardCharsets.UTF_8);
        // 阶段6补：从消息属性恢复下单时的 traceId（异步边界延续同一链路；丢了自生成，兼容旧消息），
        // 加上 orderId 自埋 —— 同步链+异步链两条线都在 Kibana 可追
        String traceId = message.getProperty(TraceIdFilter.MDC_TRACE_ID);
        MDC.put(TraceIdFilter.MDC_TRACE_ID,
                traceId != null ? traceId : UUID.randomUUID().toString().replace("-", ""));
        MDC.put(TraceIdFilter.MDC_ORDER_ID, orderId);
        try {
            boolean closed = orderService.closeOrderIfPending(Long.valueOf(orderId));
            log.info("[延迟关单] 订单{} 到点检查，{}",
                    orderId, closed ? "未支付已关闭（回补结果见关单日志）" : "无需处理（已支付/已关/不存在）");
        } finally {
            MDC.clear();
        }
    }
}
