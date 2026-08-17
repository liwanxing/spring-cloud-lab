package com.liwx.laborder.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.liwx.labcommon.common.PageResult;
import com.liwx.labcommon.common.Result;
import java.util.List;
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

    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(StpUtil.getLoginIdAsLong(), dto));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(orderService.listOrders(StpUtil.getLoginIdAsLong(), page, size, status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrder(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/checkout")
    public Result<List<OrderVO>> checkout() {
        return Result.success(orderService.checkoutFromCart(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/{id}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(StpUtil.getLoginIdAsLong(), id));
    }
}