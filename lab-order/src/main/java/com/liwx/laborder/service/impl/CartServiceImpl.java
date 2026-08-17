package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liwx.labcommon.common.Assert;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.entity.Cart;
import com.liwx.laborder.feign.ProductFeignClient;
import com.liwx.laborder.mapper.CartMapper;
import com.liwx.laborder.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long userId, CartAddDTO dto) {
        var res = productFeignClient.getProduct(dto.getProductId());
        Assert.isTrue(res.getCode() == 200, "商品不存在");
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) res.getData();

        Cart existing = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, dto.getProductId()));

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            cartMapper.updateById(existing);
        } else {
            Cart cart = Cart.builder()
                    .userId(userId).productId(dto.getProductId())
                    .productName((String) product.get("name"))
                    .productPrice(new BigDecimal(product.get("price").toString()))
                    .quantity(dto.getQuantity()).build();
            cartMapper.insert(cart);
        }
    }

    @Override
    public List<CartVO> listCart(Long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).orderByDesc(Cart::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    @Override
    public void updateQuantity(Long userId, Long cartId, CartUpdateDTO dto) {
        Cart cart = cartMapper.selectById(cartId);
        Assert.notNull(cart, "购物车项不存在");
        Assert.isTrue(cart.getUserId().equals(userId), "无权操作");
        cart.setQuantity(dto.getQuantity());
        cartMapper.updateById(cart);
    }

    @Override
    public void removeFromCart(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        Assert.notNull(cart, "购物车项不存在");
        Assert.isTrue(cart.getUserId().equals(userId), "无权操作");
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clearCart(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private CartVO toVO(Cart cart) {
        CartVO vo = new CartVO();
        BeanUtils.copyProperties(cart, vo);
        vo.setItemAmount(cart.getProductPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        return vo;
    }
}