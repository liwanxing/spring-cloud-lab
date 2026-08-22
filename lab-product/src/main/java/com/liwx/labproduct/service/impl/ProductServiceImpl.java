package com.liwx.labproduct.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.common.PageResult;
import com.liwx.labproduct.dto.*;
import com.liwx.labproduct.entity.Product;
import com.liwx.labproduct.mapper.ProductMapper;
import com.liwx.labproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
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

    /**
     * 业务语义：下单即扣（支付环节不碰库存）；取消/超时关单的回补走专属 restoreStock 接口。
     * 分支事务（RM）：无需任何 Seata 注解，收到 XID 后本地提交时自动注册分支并写 undo_log。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO deductStock(Long id, int quantity) {
        Assert.isTrue(quantity > 0, "扣减数量必须大于0");
        Assert.isTrue(productMapper.deductStock(id, quantity) > 0, "库存不足");
        ProductVO vo = getById(id);
        // 阶段6：跨服务链路的另一端 —— traceId 由 Feign 请求头从 lab-order 透传过来，
        // 这条日志在 Kibana 里与下单服务日志同 traceId，一筛即串链
        log.info("[库存扣减] 商品{} 扣{}件 剩{}件", id, quantity, vo.getStock());
        return vo;
    }

    /**
     * 回补库存：取消/超时关单加回（正数语义）。历史上用"负数扣减"魔法回补，
     * 被 quantity>0 参数校验拒绝（Feign 400），改为专属接口才是正路。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO restoreStock(Long id, int quantity) {
        Assert.isTrue(quantity > 0, "回补数量必须大于0");
        Assert.isTrue(productMapper.restoreStock(id, quantity) > 0, "商品不存在");
        ProductVO vo = getById(id);
        // 阶段6：同扣减 —— 取消/关单/对账的回补链路也可按 traceId/orderId 追溯
        log.info("[库存回补] 商品{} 补{}件 剩{}件", id, quantity, vo.getStock());
        return vo;
    }

    private ProductVO toVO(Product p) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }
}
