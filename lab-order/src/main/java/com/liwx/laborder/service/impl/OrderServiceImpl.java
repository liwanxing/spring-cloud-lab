package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.common.PageResult;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.entity.Order;
import com.liwx.laborder.entity.OrderItem;
import com.liwx.laborder.entity.Cart;
import com.liwx.laborder.feign.ProductFeignClient;
import com.liwx.laborder.mapper.CartMapper;
import com.liwx.laborder.mapper.OrderItemMapper;
import com.liwx.laborder.mapper.OrderMapper;
import com.liwx.laborder.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductFeignClient productFeignClient;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        return doCreateOrder(userId, dto.getItems());
    }

    @Override
    @Transactional
    public OrderVO createOrderFromCart(Long userId) {
        List<Cart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        Assert.isTrue(!cartItems.isEmpty(), "购物车为空");

        // 购物车商品转为统一的下单结构，复用下单内核
        List<OrderItemCreateDTO> items = cartItems.stream()
                .map(c -> new OrderItemCreateDTO(c.getProductId(), c.getQuantity()))
                .toList();
        OrderVO orderVO = doCreateOrder(userId, items);

        // 清空购物车
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return orderVO;
    }

    /**
     * 统一下单内核：查商品 -> 建主订单 -> 扣库存 -> 插明细
     * 立即购买与购物车结算共用此逻辑，天然支持一单多商品
     */
    private OrderVO doCreateOrder(Long userId, List<OrderItemCreateDTO> items) {
        // 1. 查询全部商品并计算金额（先整体校验，无任何副作用）
        List<OrderItem> orderItems = new ArrayList<>(items.size());
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemCreateDTO itemDTO : items) {
            var productRes = productFeignClient.getProduct(itemDTO.getProductId());
            Assert.isTrue(productRes.getCode() == 200, "商品不存在");
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) productRes.getData();

            BigDecimal price = new BigDecimal(product.get("price").toString());
            BigDecimal itemAmount = price.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
            orderItems.add(OrderItem.builder()
                    .productId(itemDTO.getProductId())
                    .productName((String) product.get("name"))
                    .productPrice(price)
                    .quantity(itemDTO.getQuantity())
                    .itemAmount(itemAmount)
                    .build());
        }

        // 2. 创建主订单
        Order order = Order.builder()
                .orderNo(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .totalAmount(totalAmount)
                .itemCount(orderItems.size())
                .status("PENDING")
                .deleted(0)
                .build();
        orderMapper.insert(order);

        // 3. 扣库存 + 创建订单明细
        for (OrderItem item : orderItems) {
            var deductRes = productFeignClient.deductStock(item.getProductId(), item.getQuantity());
            Assert.isTrue(deductRes.getCode() == 200, "「" + item.getProductName() + "」库存不足");
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        return toVO(order, orderItems);
    }

    @Override
    public PageResult<OrderVO> listOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null && !status.isBlank(), Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查询订单明细
        List<Long> orderIds = p.getRecords().stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderIds.isEmpty()
                ? List.of()
                : orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(OrderItem::getOrderId));

        return new PageResult<>(
                p.getRecords().stream()
                    .map(o -> toVO(o, itemsByOrder.getOrDefault(o.getId(), List.of())))
                    .toList(),
                p.getTotal(), page, size);
    }

    @Override
    public OrderVO getOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权查看");
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        return toVO(order, items);
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");
        Assert.isTrue(order.getUserId().equals(userId), "无权操作");
        Assert.isTrue("PENDING".equals(order.getStatus()), "只能取消待支付订单");

        // 取消主订单
        int rows = orderMapper.cancelOrder(orderId, userId);
        Assert.isTrue(rows > 0, "取消失败");

        // 恢复所有商品库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            try {
                productFeignClient.deductStock(item.getProductId(), -item.getQuantity());
            } catch (Exception e) { /* 库存回滚失败需消息队列兜底 */ }
        }

        order.setStatus("CANCELLED");
        return toVO(order, items);
    }

    private OrderVO toVO(Order order, List<OrderItem> items) {
        List<OrderItemVO> itemVOs = items.stream()
                .map(item -> OrderItemVO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productPrice(item.getProductPrice())
                        .quantity(item.getQuantity())
                        .itemAmount(item.getItemAmount())
                        .build())
                .toList();
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .status(order.getStatus())
                .items(itemVOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
