package com.liwx.laborder.mq;

import com.liwx.laborder.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

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
public class OrderCloseConsumer implements RocketMQListener<String> {

    private final OrderService orderService;

    @Override
    public void onMessage(String orderId) {
        boolean closed = orderService.closeOrderIfPending(Long.valueOf(orderId));
        log.info("[延迟关单] 订单{} 到点检查，{}",
                orderId, closed ? "未支付已关闭（回补结果见关单日志）" : "无需处理（已支付/已关/不存在）");
    }
}
