package com.liwx.laborder.service;

import com.liwx.labcommon.common.PageResult;
import java.util.List;
import com.liwx.laborder.dto.*;

public interface OrderService {
    OrderVO createOrder(Long userId, OrderCreateDTO dto);
    PageResult<OrderVO> listOrders(Long userId, int page, int size, String status);
    OrderVO getOrder(Long userId, Long orderId);
    OrderVO cancelOrder(Long userId, Long orderId);
    List<OrderVO> checkoutFromCart(Long userId);
}
