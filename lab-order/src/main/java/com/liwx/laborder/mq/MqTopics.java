package com.liwx.laborder.mq;

/** MQ 主题集中定义：类级注解不能引用本类成员（作用域限制），故主题常量独立成类，生产/消费共用 */
public final class MqTopics {

    /** 订单关闭检查（延迟消息）：到点后消费者核实订单状态决定是否关单 */
    public static final String ORDER_CLOSE = "order-close-topic";

    private MqTopics() {
    }
}
