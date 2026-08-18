package com.liwx.laborder.service;

import com.liwx.labcommon.common.PageResult;
import com.liwx.laborder.dto.*;

public interface OrderService {
    /** 立即购买：直接传入商品列表下单 */
    OrderVO createOrder(Long userId, OrderCreateDTO dto);
    /** 购物车结算：将购物车全部商品转为一个订单 */
    OrderVO createOrderFromCart(Long userId);
    PageResult<OrderVO> listOrders(Long userId, int page, int size, String status);
    OrderVO getOrder(Long userId, Long orderId);
    OrderVO cancelOrder(Long userId, Long orderId);

    /** 延迟消息到点关单检查：仍为 PENDING 才执行关闭，返回是否实际关闭（由 MQ 消费者触发） */
    boolean closeOrderIfPending(Long orderId);

    /** 超时关单兜底扫表：MQ 延迟消息为主、此为兜底（消息丢失/发送失败时扫表补网），由 XXL-Job 定时触发；库存回补失败留悬挂账，由对账任务 reconcileStockRestores 补 */
    int closeTimeoutOrders(int timeoutMinutes);

    /** 库存回补对账：扫"已取消但库存未回补"的悬挂账（取消时 lab-product 不可用所致），抢占销账后重试回补，由 XXL-Job 定时触发 */
    int reconcileStockRestores();
}
