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
        // 1. 查询商品信息
        var productRes = productFeignClient.getProduct(dto.getProductId());
        Assert.isTrue(productRes.getCode() == 200, "商品不存在");
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) productRes.getData();

        // 2. 扣减库存
        var deductRes = productFeignClient.deductStock(dto.getProductId(), dto.getQuantity());
        Assert.isTrue(deductRes.getCode() == 200, "库存不足");

        // 3. 计算金额
        BigDecimal price = new BigDecimal(product.get("price").toString());
        BigDecimal itemAmount = price.multiply(BigDecimal.valueOf(dto.getQuantity()));

        // 4. 创建主订单
        Order order = Order.builder()
                .orderNo(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .totalAmount(itemAmount)
                .itemCount(1)
                .status("PENDING")
                .deleted(0)
                .build();
        orderMapper.insert(order);

        // 5. 创建订单明细
        OrderItem item = OrderItem.builder()
                .orderId(order.getId())
                .productId(dto.getProductId())
                .productName((String) product.get("name"))
                .productPrice(price)
                .quantity(dto.getQuantity())
                .itemAmount(itemAmount)
                .build();
        orderItemMapper.insert(item);

        return toVO(order, List.of(item));
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

    @Override
    @Transactional
    public List<OrderVO> checkoutFromCart(Long userId) {
        List<Cart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        Assert.isTrue(!cartItems.isEmpty(), "购物车为空");

        // 1. 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart c : cartItems) {
            totalAmount = totalAmount.add(c.getProductPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }

        // 2. 创建一个主订单
        Order order = Order.builder()
                .orderNo(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .totalAmount(totalAmount)
                .itemCount(cartItems.size())
                .status("PENDING")
                .deleted(0)
                .build();
        orderMapper.insert(order);

        // 3. 扣库存 + 创建订单明细
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : cartItems) {
            var deductRes = productFeignClient.deductStock(cart.getProductId(), cart.getQuantity());
            Assert.isTrue(deductRes.getCode() == 200, "「" + cart.getProductName() + "」库存不足");

            BigDecimal itemAmount = cart.getProductPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .orderId(order.getId())
                    .productId(cart.getProductId())
                    .productName(cart.getProductName())
                    .productPrice(cart.getProductPrice())
                    .quantity(cart.getQuantity())
                    .itemAmount(itemAmount)
                    .build();
            orderItemMapper.insert(item);
            orderItems.add(item);
        }

        // 4. 清空购物车
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));

        return List.of(toVO(order, orderItems));
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
