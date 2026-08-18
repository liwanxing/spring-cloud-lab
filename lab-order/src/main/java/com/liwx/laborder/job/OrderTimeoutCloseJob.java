package com.liwx.laborder.job;

import com.liwx.laborder.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 超时关单任务：定时扫描 PENDING 超时订单，经渠道确认后关闭并回补库存。
 *
 * 控制台两块页面的分工：「执行器管理」按 AppName 发现在线机器（对应 XxlJobConfig 的 appname，
 *                   「任务管理」= 执行器 + JobHandler + Cron，
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseJob {

    private final OrderService orderService;

    @Value("${order.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    @XxlJob("orderTimeoutCloseHandler")
    public void orderTimeoutClose() {
        int timeout = payTimeoutMinutes;
        // 任务参数在控制台「任务参数」框直接填分钟数；接入 MQ 延迟关单后填 MQ延迟+容忍带（如 30+10=40），
        // 本任务退为兜底，只接 MQ 漏掉的订单
        String param = XxlJobHelper.getJobParam();
        if (param != null && !param.isBlank()) {
            try {
                timeout = Integer.parseInt(param.trim());
            } catch (NumberFormatException e) {
                XxlJobHelper.log("任务参数非数字，使用默认阈值：{}", payTimeoutMinutes);
            }
        }
        XxlJobHelper.log("超时关单开始，阈值：{} 分钟", timeout);
        int closed = orderService.closeTimeoutOrders(timeout);
        XxlJobHelper.log("超时关单完成，本轮关闭：{} 笔", closed);
        XxlJobHelper.handleSuccess("本轮关闭 " + closed + " 笔");
    }
}
