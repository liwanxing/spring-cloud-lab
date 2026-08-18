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

    /** 超时关单：关闭创建超过 timeoutMinutes 分钟仍未支付的订单，返回本轮关闭笔数（由 XXL-Job 定时触发） */
    int closeTimeoutOrders(int timeoutMinutes);
}
