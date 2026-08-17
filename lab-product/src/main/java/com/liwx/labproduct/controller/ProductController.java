package com.liwx.labproduct.controller;
import com.liwx.labcommon.common.PageResult;
import com.liwx.labcommon.common.Result;
import com.liwx.labproduct.dto.*;
import com.liwx.labproduct.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Result<PageResult<ProductVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.listProducts(page, size, name, status));
    }

    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return Result.success(productService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        return Result.success(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/deduct")
    public Result<ProductVO> deductStock(@PathVariable Long id, @RequestParam int quantity) {
        return Result.success(productService.deductStock(id, quantity));
    }
}
