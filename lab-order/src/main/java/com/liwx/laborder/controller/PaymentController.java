package com.liwx.laborder.controller;

import com.liwx.labcommon.auth.CurrentUserId;
import com.liwx.labcommon.common.Result;
import com.liwx.laborder.dto.PaymentVO;
import com.liwx.laborder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /** 发起支付：返回支付宝收银台跳转链接（mock 渠道直接成功） */
    @PostMapping("/{orderId}")
    public Result<PaymentVO> createPayment(@PathVariable Long orderId, @CurrentUserId Long userId) {
        return Result.success(paymentService.createPayment(userId, orderId));
    }

    /** 支付状态查询：前端轮询用，PAYING 时会主动向支付宝查单补偿 */
    @GetMapping("/status/{orderId}")
    public Result<String> queryStatus(@PathVariable Long orderId, @CurrentUserId Long userId) {
        return Result.success(paymentService.queryStatus(userId, orderId));
    }

    /**
     * 支付宝异步通知：免登录（支付宝服务器回调，无用户会话），
     * 验签在 Service 内完成，按支付宝规范返回纯文本 success / failure。
     */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        return paymentService.handleNotify(params) ? "success" : "failure";
    }
}
