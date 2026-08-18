package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.common.PageResult;
import com.liwx.laborder.dto.*;
import com.liwx.laborder.entity.Order;
import com.liwx.laborder.entity.OrderItem;
import com.liwx.laborder.entity.Cart;
import com.liwx.laborder.entity.Payment;
import com.liwx.laborder.feign.ProductFeignClient;
import com.liwx.laborder.mapper.CartMapper;
import com.liwx.laborder.mapper.OrderItemMapper;
import com.liwx.laborder.mapper.OrderMapper;
import com.liwx.laborder.mapper.PaymentMapper;
import com.liwx.laborder.service.OrderService;
import com.liwx.laborder.service.PaymentService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductFeignClient productFeignClient;
    private final PaymentMapper paymentMapper;
    private final PaymentService paymentService;

    /**
     * 全局事务发起方（TM）：@GlobalTransactional 开启跨服务事务，XID 随 Feign 传给 lab-product；
     * 任一环节异常（如第二件商品扣减失败）时 TC 指挥各库 undo_log 逆向补偿，库存自动弹回。
     * 注解只能挂入口方法：挂私有内核 doCreateOrder 会因 AOP 自调用失效。
     *
     * 事务分工（Seata 是兜底的）：本地事务没提交的分支，InnoDB 自己 rollback 擦干净
     * （连 undo_log 一并回滚，不留记录）；只有已本地提交的分支才留下 undo_log，
     * 由 Seata 反向补偿。判定只看提交到哪一步，与谁报错无关。
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        return doCreateOrder(userId, dto.getItems());
    }

    /** 同 createOrder：购物车入口的全局事务边界 */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
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
        List<OrderItem> items = restoreStock(orderId);

        order.setStatus("CANCELLED");
        return toVO(order, items);
    }

    /**
     * 超时关单：扫描超时未支付订单，逐笔处理（渠道确认 -> 条件取消 -> 回补库存），
     * 单笔失败不影响其余订单，渠道侧异常等下一轮任务重试。
     */
    @Override
    public int closeTimeoutOrders(int timeoutMinutes) {
        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PENDING")
                .lt(Order::getCreatedAt, LocalDateTime.now().minusMinutes(timeoutMinutes))
                .orderByAsc(Order::getId)
                .last("LIMIT 50"));
        int closed = 0;
        for (Order order : timeoutOrders) {
            try {
                if (closeOneTimeoutOrder(order)) {
                    closed++;
                }
            } catch (Exception e) {
                log.error("[超时关单] 订单{} 处理异常，本轮跳过", order.getOrderNo(), e);
            }
        }
        return closed;
    }

    /** 单笔关单：存在未完成支付流水时先经渠道确认/关闭，再条件取消订单并回补库存 */
    private boolean closeOneTimeoutOrder(Order order) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getStatus, "PAYING")
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        // 渠道侧已支付（已转支付成功）或渠道交互失败时不可关单，等下一轮
        if (payment != null && !paymentService.closeTrade(payment)) {
            return false;
        }
        int rows = orderMapper.cancelOrder(order.getId(), order.getUserId());
        if (rows <= 0) {
            // 并发下已被支付/取消，无需处理
            return false;
        }
        restoreStock(order.getId());
        log.info("[超时关单] 订单{} 超时未支付已关闭，库存已回补", order.getOrderNo());
        return true;
    }

    /** 回补订单全部商品库存：尽力而为，失败记日志等待后续补偿（与手动取消共用） */
    private List<OrderItem> restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            try {
                productFeignClient.deductStock(item.getProductId(), -item.getQuantity());
            } catch (Exception e) {
                log.error("[库存回补] 失败，等待补偿。productId={} quantity={}",
                        item.getProductId(), item.getQuantity(), e);
            }
        }
        return items;
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
                .paidAt(order.getPaidAt())
                .items(itemVOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
