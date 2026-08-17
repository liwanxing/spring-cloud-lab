package com.liwx.laborder.service;
import com.liwx.laborder.dto.*;
import java.util.List;
public interface CartService {
    void addToCart(Long userId, CartAddDTO dto);
    List<CartVO> listCart(Long userId);
    void updateQuantity(Long userId, Long cartId, CartUpdateDTO dto);
    void removeFromCart(Long userId, Long cartId);
    void clearCart(Long userId);
}