package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.common.PageResult;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.entity.Order;
import com.liwx.laborder.feign.ProductFeignClient;
import com.liwx.laborder.mapper.OrderMapper;
import com.liwx.laborder.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final ProductFeignClient productFeignClient;

    @Override
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        var productRes = productFeignClient.getProduct(dto.getProductId());
        Assert.isTrue(productRes.getCode() == 200, "商品不存在");
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) productRes.getData();

        var deductRes = productFeignClient.deductStock(dto.getProductId(), dto.getQuantity());
        Assert.isTrue(deductRes.getCode() == 200, "库存不足");

        BigDecimal price = new BigDecimal(product.get("price").toString());
        Order order = Order.builder()
                .orderNo(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .productId(dto.getProductId())
                .productName((String) product.get("name"))
                .productPrice(price)
                .quantity(dto.getQuantity())
                .totalAmount(price.multiply(BigDecimal.valueOf(dto.getQuantity())))
                .status("PENDING")
                .deleted(0)
                .build();
        orderMapper.insert(order);
        return toVO(order);
    }

    @Override
    public PageResult<OrderVO> listOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null && !status.isBlank(), Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    @Override
    public OrderVO getOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权查看");
        return toVO(order);
    }

    @Override
    public OrderVO cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权操作");
        Assert.isTrue("PENDING".equals(order.getStatus()), "只能取消待支付订单");
        int rows = orderMapper.cancelOrder(orderId, userId);
        Assert.isTrue(rows > 0, "取消失败");
        try { productFeignClient.deductStock(order.getProductId(), -order.getQuantity()); }
        catch (Exception e) { /* 库存回滚失败需消息队列兜底 */ }
        order.setStatus("CANCELLED");
        return toVO(order);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}