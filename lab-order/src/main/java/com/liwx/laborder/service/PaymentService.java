package com.liwx.laborder.service;

import com.liwx.laborder.dto.PaymentVO;
import java.util.Map;

/**
 * 支付渠道抽象：订单侧只依赖此接口，不关心具体支付方式。
 * 实现类由 payment.channel 配置切换：alipay（支付宝沙箱）/ mock（模拟支付，默认）。
 * 真实支付是异步的：createPayment 只发起支付并落 PAYING 流水，
 * 最终状态由异步回调（handleNotify）或主动查单（queryStatus）推进。
 */
public interface PaymentService {

    /** 发起支付：落支付流水并返回支付跳转链接 */
    PaymentVO createPayment(Long userId, Long orderId);

    /** 查询支付状态：本地流水优先，PAYING 时主动向渠道查单补偿（回调丢失兜底） */
    String queryStatus(Long userId, Long orderId);

    /** 处理支付宝异步通知：验签 + 金额校验 + 推进流水与订单状态，返回是否处理成功 */
    boolean handleNotify(Map<String, String> params);
}
