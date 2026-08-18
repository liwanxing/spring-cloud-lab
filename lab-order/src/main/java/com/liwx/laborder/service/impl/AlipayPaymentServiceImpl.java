package com.liwx.laborder.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.exception.BusinessException;
import com.liwx.laborder.config.AlipayProperties;
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
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝沙箱支付（电脑网站支付 alipay.trade.page.pay）。
 * 流程：createPayment 落 PAYING 流水并返回收银台链接 -> 用户扫码付款 ->
 * 异步回调 handleNotify（验签）或前端轮询触发 queryStatus（主动查单）推进状态。
 * payment.channel=alipay 时生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.channel", havingValue = "alipay")
public class AlipayPaymentServiceImpl implements PaymentService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Override
    public PaymentVO createPayment(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权操作");
        Assert.isTrue("PENDING".equals(order.getStatus()), "订单状态不允许支付");

        // 复用未完成的支付单，避免同一订单重复发起产生多条 PAYING 流水
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, "PAYING")
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            payment = Payment.builder()
                    .payNo(UUID.randomUUID().toString().replace("-", ""))
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .channel("ALIPAY")
                    .amount(order.getTotalAmount())
                    .status("PAYING")
                    .build();
            paymentMapper.insert(payment);
        }

        // 电脑网站支付：pageExecute(GET) 直接返回收银台跳转链接
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        if (StringUtils.hasText(alipayProperties.getNotifyUrl())) {
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
        }
        if (StringUtils.hasText(alipayProperties.getReturnUrl())) {
            request.setReturnUrl(alipayProperties.getReturnUrl());
        }
        request.setBizContent("{\"out_trade_no\":\"" + payment.getPayNo() + "\","
                + "\"total_amount\":\"" + order.getTotalAmount() + "\","
                + "\"subject\":\"商城订单-" + order.getOrderNo() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        try {
            String payUrl = alipayClient.pageExecute(request, "GET").getBody();
            log.info("[支付宝] 订单{} 发起支付，支付单号：{}", order.getOrderNo(), payment.getPayNo());
            return PaymentVO.builder()
                    .payNo(payment.getPayNo())
                    .payUrl(payUrl)
                    .status(payment.getStatus())
                    .build();
        } catch (AlipayApiException e) {
            log.error("[支付宝] 发起支付失败：{}", e.getErrMsg(), e);
            throw new BusinessException("发起支付失败：" + e.getErrMsg());
        }
    }

    @Override
    public String queryStatus(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权查询");
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            return "NONE";
        }
        if (!"PAYING".equals(payment.getStatus())) {
            return payment.getStatus();
        }

        // 本地还是 PAYING，主动向支付宝查单（回调丢失/未配公网回调时的兜底）
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + payment.getPayNo() + "\"}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()
                    && ("TRADE_SUCCESS".equals(response.getTradeStatus())
                        || "TRADE_FINISHED".equals(response.getTradeStatus()))) {
                completePayment(payment, response.getTradeNo());
            }
        } catch (AlipayApiException e) {
            log.warn("[支付宝] 查单失败，支付单号{}：{}", payment.getPayNo(), e.getErrMsg());
        }
        return payment.getStatus();
    }

    @Override
    @Transactional
    public boolean handleNotify(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        try {
            // 1. 验签：确保通知确实来自支付宝且未被篡改
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayProperties.getAlipayPublicKey(), "UTF-8", "RSA2");
            if (!signVerified) {
                log.warn("[支付宝] 异步通知验签失败，out_trade_no={}", outTradeNo);
                return false;
            }

            // 2. 只处理支付成功类通知，其余（如等待买家付款）直接确认
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return true;
            }

            Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getPayNo, outTradeNo));
            if (payment == null) {
                log.warn("[支付宝] 通知对应的支付单不存在，out_trade_no={}", outTradeNo);
                return false;
            }

            // 3. 金额校验：通知金额必须与本地流水一致
            BigDecimal notifyAmount = new BigDecimal(params.get("total_amount"));
            if (payment.getAmount().compareTo(notifyAmount) != 0) {
                log.error("[支付宝] 通知金额与流水不一致！out_trade_no={} 本地={} 通知={}",
                        outTradeNo, payment.getAmount(), notifyAmount);
                return false;
            }

            // 4. 推进流水与订单状态（幂等）
            completePayment(payment, params.get("trade_no"));
            log.info("[支付宝] 订单{} 支付成功，交易号：{}", payment.getOrderNo(), params.get("trade_no"));
            return true;
        } catch (Exception e) {
            log.error("[支付宝] 处理异步通知异常，out_trade_no={}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean closeTrade(Payment payment) {
        // 1. 先查单：关单瞬间用户可能刚好付款，已付则转为支付成功，绝不关已付交易（钱货两失）
        try {
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            queryRequest.setBizContent("{\"out_trade_no\":\"" + payment.getPayNo() + "\"}");
            AlipayTradeQueryResponse queryResponse = alipayClient.execute(queryRequest);
            if (queryResponse.isSuccess()
                    && ("TRADE_SUCCESS".equals(queryResponse.getTradeStatus())
                        || "TRADE_FINISHED".equals(queryResponse.getTradeStatus()))) {
                completePayment(payment, queryResponse.getTradeNo());
                log.info("[支付宝] 超时关单查单发现已支付，转为支付成功，支付单号：{}", payment.getPayNo());
                return false;
            }
        } catch (AlipayApiException e) {
            // 查单失败不关单，等下一轮任务重试（宁可多等一轮，不可错关）
            log.warn("[支付宝] 超时关单查单失败，本轮跳过，支付单号{}：{}", payment.getPayNo(), e.getErrMsg());
            return false;
        }

        // 2. 渠道侧未支付，关闭支付宝交易；交易不存在（用户从未打开收银台）视为已关闭，其余失败跳过等下轮
        try {
            AlipayTradeCloseRequest closeRequest = new AlipayTradeCloseRequest();
            closeRequest.setBizContent("{\"out_trade_no\":\"" + payment.getPayNo() + "\"}");
            AlipayTradeCloseResponse closeResponse = alipayClient.execute(closeRequest);
            if (!closeResponse.isSuccess() && !"ACQ.TRADE_NOT_EXIST".equals(closeResponse.getSubCode())) {
                log.warn("[支付宝] 关闭交易失败，本轮跳过，支付单号{}：{}", payment.getPayNo(), closeResponse.getSubMsg());
                return false;
            }
        } catch (AlipayApiException e) {
            log.warn("[支付宝] 关闭交易异常，本轮跳过，支付单号{}：{}", payment.getPayNo(), e.getErrMsg());
            return false;
        }

        // 3. 本地流水置 CLOSED（幂等条件更新，仅 PAYING 可置）
        paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, "PAYING")
                .set(Payment::getStatus, "CLOSED"));
        log.info("[支付宝] 超时未支付，交易已关闭，支付单号：{}", payment.getPayNo());
        return true;
    }

    /** 流水置 SUCCESS 并条件更新订单为 PAID，两侧均为幂等条件更新 */
    private void completePayment(Payment payment, String tradeNo) {
        paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, "PAYING")
                .set(Payment::getStatus, "SUCCESS")
                .set(Payment::getTradeNo, tradeNo));
        payment.setStatus("SUCCESS");
        payment.setTradeNo(tradeNo);
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            // 条件更新：仅 PENDING 可置为 PAID，天然防止重复支付/取消后回调到达
            orderMapper.payOrder(order.getId(), order.getUserId());
        }
    }
}
