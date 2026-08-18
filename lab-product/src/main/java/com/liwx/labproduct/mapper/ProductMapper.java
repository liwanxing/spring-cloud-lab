package com.liwx.labproduct.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liwx.labproduct.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 回补库存：取消/超时关单时加回，与 deductStock 对称（均只动自己的行，不带 stock 上限约束） */
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}