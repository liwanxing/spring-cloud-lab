package com.liwx.laborder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.exception.BusinessException;
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
import com.liwx.laborder.mq.MqTopics;
import com.liwx.laborder.service.OrderService;
import com.liwx.laborder.service.PaymentService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.messaging.support.MessageBuilder;
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
    private final RocketMQTemplate rocketMQTemplate;

    /** 延迟关单档位（RocketMQ 经典 18 档：5=1m 14=10m 16=30m），实验用 1 分钟 */
    @Value("${order.close-delay-level:5}")
    private int closeDelayLevel;

    /** 回补对账缓冲期（分钟）：刚取消的单可能正常路径正在回补中，过缓冲期仍是悬挂账才动手，防撞车双回补 */
    @Value("${order.restore-reconcile-grace-minutes:2}")
    private int restoreReconcileGraceMinutes;

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
        // 购物车页面为全量结算设计：无商品勾选功能，一次结算全部商品并整单清空购物车
        // （不想要的商品需先在购物车页面上删除），故此处直接查全量，不接收选中项参数
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
        // 1. 查询全部商品并计算金额（先整体校验，无任何副作用）；
        // 前端展示的价格不可信（可被篡改），金额一律由服务端实时查询商品现价计算，
        List<OrderItem> orderItems = new ArrayList<>(items.size());
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemCreateDTO itemDTO : items) {
            var productRes = productFeignClient.getProduct(itemDTO.getProductId());
            // 503 = Sentinel 降级（lab-product 不可用/熔断）：抛 503 业务异常，HTTP 状态同步透传，与"商品真不存在"区分
            if (productRes.getCode() == 503) {
                throw new BusinessException(503, productRes.getMessage());
            }
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
            // 同上：降级 503 抛独立异常透传 503 语义，避免误报"库存不足"
            if (deductRes.getCode() == 503) {
                throw new BusinessException(503, deductRes.getMessage());
            }
            Assert.isTrue(deductRes.getCode() == 200, "「" + item.getProductName() + "」库存不足");
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 4. 发延迟关单消息：到点后消费者检查该单，仍 PENDING 才关（双入口共用内核，天然全覆盖）
        // 注：消息在全局事务内先飞出，若全局回滚则订单不存在，消费者空跳忽略；
        // 发送失败仅记日志不影响下单成功 —— 关单另有 XXL-Job 扫表兜底
        try {
            rocketMQTemplate.syncSend(MqTopics.ORDER_CLOSE,
                    MessageBuilder.withPayload(String.valueOf(order.getId())).build(),
                    3000, closeDelayLevel);
        } catch (Exception e) {
            log.error("[延迟关单] 订单{} 消息发送失败，等待扫表兜底", order.getId(), e);
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

        // 恢复所有商品库存：全部成功则销账（stock_restored 0->1）；
        // 有失败项则留悬挂账（0），由对账任务 StockRestoreReconcileJob 等服务恢复后重试回补
        boolean restored = restoreStock(orderId);
        if (restored) {
            orderMapper.claimStockRestore(orderId);
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (!restored) {
            log.warn("[手动取消] 订单{} 库存回补失败，已挂账等对账任务补偿", orderId);
        }

        order.setStatus("CANCELLED");
        return toVO(order, items);
    }

    /**
     * 超时关单兜底扫表：正常路径由 MQ 延迟消息到点关单，本任务兜住消息丢失/发送失败的单。
     * 扫描超时未支付订单，逐笔处理（渠道确认 -> 条件取消 -> 回补库存），
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
        // 全部回补成功则销账；失败项留悬挂账，由对账任务重试回补
        boolean restored = restoreStock(order.getId());
        if (restored) {
            orderMapper.claimStockRestore(order.getId());
            log.info("[超时关单] 订单{} 超时未支付已关闭，库存已回补", order.getOrderNo());
        } else {
            log.warn("[超时关单] 订单{} 超时未支付已关闭，库存回补失败已挂账，等对账任务补偿", order.getOrderNo());
        }
        return true;
    }

    /** 延迟消息到点检查：仍为 PENDING 才关（复用单笔关单内核），返回是否实际关闭 */
    @Override
    public boolean closeOrderIfPending(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            return false;   // 不存在（全局回滚）/已支付/已关，消息空跳
        }
        return closeOneTimeoutOrder(order);
    }

    /**
     * 库存回补对账（悬挂账清理）：取消订单时若 lab-product 恰好不可用（降级 503），回补失败留下
     * "已取消但库存未回补"的悬挂账（stock_restored=0）。本方法扫出这类单，抢占销账后重试回补。
     *
     * 防双回补三道闸（回补加库存不幂等，双回补=库存虚增=可超卖）：
     * 1) 缓冲期：刚取消的单可能正常路径正在回补中（MQ 关单/手动取消），过缓冲期仍是 0 才算真悬挂；
     * 2) CAS 抢占：claimStockRestore 条件更新 0->1，抢到回补权才动手；
     * 3) 失败退账：抢占后仍补不动则释放（1->0），下一轮重来。
     * 已知限制：整单重试不区分明细 —— 若回补到一半服务再挂，重试会重复回补已成功的明细；
     * 生产做法是明细级幂等键（或 restore 接口按 orderId 去重），本仓库从简。
     */
    @Override
    public int reconcileStockRestores() {
        List<Order> pending = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "CANCELLED")
                .eq(Order::getStockRestored, 0)
                .lt(Order::getUpdatedAt, LocalDateTime.now().minusMinutes(restoreReconcileGraceMinutes))
                .orderByAsc(Order::getId)
                .last("LIMIT 50"));
        int settled = 0;
        for (Order order : pending) {
            if (orderMapper.claimStockRestore(order.getId()) == 0) {
                continue;   // 并发已被其他线程/实例抢走
            }
            if (restoreStock(order.getId())) {
                settled++;
                log.info("[回补对账] 订单{} 悬挂账已销账，库存回补完成", order.getOrderNo());
            } else {
                orderMapper.releaseStockRestoreClaim(order.getId());
                log.warn("[回补对账] 订单{} 回补仍失败，已退账等下一轮", order.getOrderNo());
            }
        }
        return settled;
    }

    /**
     * 回补订单全部商品库存（走 lab-product 专属 restore 接口，正数加回）：
     * 尽力而为，单项失败（异常或降级 503）记错误日志待补偿，返回是否全部成功（手动取消/超时关单/回补对账三处共用）。
     * 注：Feign 开 Sentinel 降级后异常被 fallback 吞掉只返回 503 Result，必须校验返回码，
     * 否则降级会被误报成"已回补"（又一层日志说谎风险）。
     */
    private boolean restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        boolean allRestored = true;
        for (OrderItem item : items) {
            try {
                var res = productFeignClient.restoreStock(item.getProductId(), item.getQuantity());
                if (res == null || res.getCode() != 200) {
                    allRestored = false;
                    log.error("[库存回补] 失败（服务降级），等待补偿。productId={} quantity={} res={}",
                            item.getProductId(), item.getQuantity(), res);
                }
            } catch (Exception e) {
                allRestored = false;
                log.error("[库存回补] 失败，等待补偿。productId={} quantity={}",
                        item.getProductId(), item.getQuantity(), e);
            }
        }
        return allRestored;
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
