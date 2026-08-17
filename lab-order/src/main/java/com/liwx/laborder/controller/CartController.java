package com.liwx.laborder.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.liwx.labcommon.common.Result;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping
    public Result<Void> addToCart(@Valid @RequestBody CartAddDTO dto) {
        cartService.addToCart(StpUtil.getLoginIdAsLong(), dto);
        return Result.success(null);
    }

    @GetMapping
    public Result<List<CartVO>> listCart() {
        return Result.success(cartService.listCart(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(StpUtil.getLoginIdAsLong(), id);
        return Result.success(null);
    }

    @DeleteMapping
    public Result<Void> clearCart() {
        cartService.clearCart(StpUtil.getLoginIdAsLong());
        return Result.success(null);
    }
}