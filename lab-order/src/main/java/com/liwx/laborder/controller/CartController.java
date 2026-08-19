package com.liwx.laborder.controller;

import com.liwx.labcommon.auth.CurrentUserId;
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
    public Result<Void> addToCart(@Valid @RequestBody CartAddDTO dto, @CurrentUserId Long userId) {
        cartService.addToCart(userId, dto);
        return Result.success(null);
    }

    @GetMapping
    public Result<List<CartVO>> listCart(@CurrentUserId Long userId) {
        return Result.success(cartService.listCart(userId));
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto, @CurrentUserId Long userId) {
        cartService.updateQuantity(userId, id, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id, @CurrentUserId Long userId) {
        cartService.removeFromCart(userId, id);
        return Result.success(null);
    }

    @DeleteMapping
    public Result<Void> clearCart(@CurrentUserId Long userId) {
        cartService.clearCart(userId);
        return Result.success(null);
    }
}