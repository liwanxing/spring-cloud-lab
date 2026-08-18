package com.liwx.laborder.job;

import com.liwx.laborder.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 库存回补对账任务：清理"已取消但库存未回补"的悬挂账（orders.stock_restored=0 的 CANCELLED 单）。
 * 悬挂账成因：取消/超时关单那一刻 lab-product 恰好不可用（Sentinel 降级 503），
 * 订单关得掉、库存补不回 —— 由本任务等商品服务恢复后重试回补。
 * 控制台配置：JobHandler=stockRestoreReconcileHandler，建议每分钟，路由策略 FIRST。
 */
@Component
@RequiredArgsConstructor
public class StockRestoreReconcileJob {

    private final OrderService orderService;

    @XxlJob("stockRestoreReconcileHandler")
    public void stockRestoreReconcile() {
        XxlJobHelper.log("库存回补对账开始");
        int settled = orderService.reconcileStockRestores();
        XxlJobHelper.log("库存回补对账完成，本轮销账：{} 笔", settled);
        XxlJobHelper.handleSuccess("本轮销账 " + settled + " 笔");
    }
}
