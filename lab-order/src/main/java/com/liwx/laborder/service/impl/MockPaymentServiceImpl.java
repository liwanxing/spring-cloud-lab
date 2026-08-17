package com.liwx.laborder.service.impl;

import com.liwx.laborder.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * 模拟支付：直接返回成功。
 * 后续接入支付宝沙箱时，新建 AlipayPaymentServiceImpl 并加 @Primary 替换本实现。
 */
@Slf4j
@Service
public class MockPaymentServiceImpl implements PaymentService {

    @Override
    public boolean pay(String orderNo, BigDecimal amount) {
        log.info("[Mock支付] 订单{} 支付成功，金额：{}", orderNo, amount);
        return true;
    }
}
