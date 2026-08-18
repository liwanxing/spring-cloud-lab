package com.liwx.laborder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liwx.laborder.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    int cancelOrder(@Param("id") Long id, @Param("userId") Long userId);

    /** 条件更新为已支付：仅 PENDING 状态可支付，防止并发重复支付 */
    int payOrder(@Param("id") Long id, @Param("userId") Long userId);

    /** 抢占销账（CAS 0->1）：仅已取消且未回补的单可置 1；返回 1=抢到回补权（才能动手回补），0=已被处理 */
    int claimStockRestore(@Param("id") Long id);

    /** 退账（1->0）：抢占后回补仍失败时归还，留待对账任务下一轮重试 */
    int releaseStockRestoreClaim(@Param("id") Long id);
}
