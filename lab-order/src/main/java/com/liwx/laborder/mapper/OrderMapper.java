package com.liwx.laborder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liwx.laborder.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    int cancelOrder(@Param("id") Long id, @Param("userId") Long userId);
}
