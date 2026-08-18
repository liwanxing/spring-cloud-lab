package com.liwx.laborder.feign;
import com.liwx.labcommon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "lab-product", fallback = ProductFeignFallback.class)
public interface ProductFeignClient {
    @GetMapping("/api/products/{id}")
    Result<?> getProduct(@PathVariable("id") Long id);
    @PutMapping("/api/products/{id}/deduct")
    Result<?> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    /** 回补库存（取消/超时关单）：正数加回，走 lab-product 专属 restore 接口 */
    @PutMapping("/api/products/{id}/restore")
    Result<?> restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}