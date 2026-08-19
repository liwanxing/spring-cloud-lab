package com.liwx.laborder.controller;

import com.liwx.labcommon.auth.CurrentUserId;
import com.liwx.labcommon.common.PageResult;
import com.liwx.labcommon.common.Result;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /** 立即购买：直接传入商品列表下单 */
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto, @CurrentUserId Long userId) {
        return Result.success(orderService.createOrder(userId, dto));
    }

    /** 购物车结算：将购物车全部商品转为一个订单 */
    @PostMapping("/from-cart")
    public Result<OrderVO> createOrderFromCart(@CurrentUserId Long userId) {
        return Result.success(orderService.createOrderFromCart(userId));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status, @CurrentUserId Long userId) {
        return Result.success(orderService.listOrders(userId, page, size, status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getOrder(@PathVariable Long id, @CurrentUserId Long userId) {
        return Result.success(orderService.getOrder(userId, id));
    }

    @PutMapping("/{id}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable Long id, @CurrentUserId Long userId) {
        return Result.success(orderService.cancelOrder(userId, id));
    }
}