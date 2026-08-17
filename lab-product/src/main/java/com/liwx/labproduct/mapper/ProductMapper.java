package com.liwx.labproduct.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liwx.labproduct.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}