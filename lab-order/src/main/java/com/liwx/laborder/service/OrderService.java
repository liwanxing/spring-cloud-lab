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
    /** 支付订单：仅 PENDING 可支付，防重复支付 */
    OrderVO payOrder(Long userId, Long orderId);
    OrderVO cancelOrder(Long userId, Long orderId);
}
