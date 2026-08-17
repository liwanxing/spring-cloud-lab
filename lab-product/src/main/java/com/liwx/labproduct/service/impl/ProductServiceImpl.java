package com.liwx.labproduct.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labproduct.common.Assert;
import com.liwx.labproduct.common.PageResult;
import com.liwx.labproduct.dto.*;
import com.liwx.labproduct.entity.Product;
import com.liwx.labproduct.mapper.ProductMapper;
import com.liwx.labproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;

    @Override
    public PageResult<ProductVO> listProducts(int page, int size, String name, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(name != null && !name.isBlank(), Product::getName, name)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt);
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    @Override
    public ProductVO getById(Long id) {
        Product p = productMapper.selectById(id);
        Assert.notNull(p, "商品不存在");
        return toVO(p);
    }

    @Override
    public ProductVO create(ProductCreateDTO dto) {
        Product p = new Product();
        BeanUtils.copyProperties(dto, p);
        p.setStatus(1);
        p.setDeleted(0);
        productMapper.insert(p);
        return toVO(p);
    }

    @Override
    public ProductVO update(Long id, ProductUpdateDTO dto) {
        Product p = productMapper.selectById(id);
        Assert.notNull(p, "商品不存在");
        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getPrice() != null) p.setPrice(dto.getPrice());
        if (dto.getStock() != null) p.setStock(dto.getStock());
        if (dto.getCategory() != null) p.setCategory(dto.getCategory());
        if (dto.getStatus() != null) p.setStatus(dto.getStatus());
        p.setUpdatedAt(null);
        productMapper.updateById(p);
        return toVO(p);
    }

    @Override
    public void delete(Long id) {
        Assert.isTrue(productMapper.selectById(id) != null, "商品不存在");
        productMapper.deleteById(id);
    }

    @Override
    public ProductVO deductStock(Long id, int quantity) {
        Assert.isTrue(quantity > 0, "扣减数量必须大于0");
        Assert.isTrue(productMapper.deductStock(id, quantity) > 0, "库存不足");
        return getById(id);
    }

    private ProductVO toVO(Product p) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }
}
