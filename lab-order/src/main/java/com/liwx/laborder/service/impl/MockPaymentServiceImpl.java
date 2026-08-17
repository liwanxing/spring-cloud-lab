package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liwx.labcommon.common.Assert;
import com.liwx.laborder.dto.PaymentVO;
import com.liwx.laborder.entity.Order;
import com.liwx.laborder.entity.Payment;
import com.liwx.laborder.mapper.OrderMapper;
import com.liwx.laborder.mapper.PaymentMapper;
import com.liwx.laborder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

/**
 * 模拟支付：发起即成功，无真实渠道交互。
 * payment.channel 未配置或为 mock 时生效，便于脱离支付宝联调业务闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.channel", havingValue = "mock", matchIfMissing = true)
public class MockPaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public PaymentVO createPayment(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权操作");
        Assert.isTrue("PENDING".equals(order.getStatus()), "订单状态不允许支付");

        Payment payment = Payment.builder()
                .payNo(UUID.randomUUID().toString().replace("-", ""))
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .channel("MOCK")
                .amount(order.getTotalAmount())
                .status("PAYING")
                .build();
        paymentMapper.insert(payment);

        // 模拟渠道同步返回支付成功
        log.info("[Mock支付] 订单{} 支付成功，金额：{}", order.getOrderNo(), order.getTotalAmount());
        completePayment(payment, "MOCK-" + payment.getPayNo());
        return PaymentVO.builder()
                .payNo(payment.getPayNo())
                .payUrl(null)
                .status(payment.getStatus())
                .build();
    }

    @Override
    public String queryStatus(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权查询");
        Payment payment = latestPayment(orderId);
        return payment == null ? "NONE" : payment.getStatus();
    }

    @Override
    public boolean handleNotify(Map<String, String> params) {
        // 模拟渠道无回调
        return true;
    }

    /** 流水置 SUCCESS 并条件更新订单为 PAID */
    private void completePayment(Payment payment, String tradeNo) {
        payment.setStatus("SUCCESS");
        payment.setTradeNo(tradeNo);
        paymentMapper.updateById(payment);
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            orderMapper.payOrder(order.getId(), order.getUserId());
        }
    }

    private Payment latestPayment(Long orderId) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
    }
}
