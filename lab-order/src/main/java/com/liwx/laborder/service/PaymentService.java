package com.liwx.laborder.service;

import java.math.BigDecimal;

/**
 * 支付渠道抽象：订单侧只依赖此接口，不关心具体支付方式。
 * 当前为 Mock 实现；接入支付宝沙箱时新增实现类并替换即可，订单代码不动。
 */
public interface PaymentService {
    /**
     * 执行支付
     * @param orderNo 商户订单号（订单号）
     * @param amount 支付金额
     * @return 支付是否成功
     */
    boolean pay(String orderNo, BigDecimal amount);
}
