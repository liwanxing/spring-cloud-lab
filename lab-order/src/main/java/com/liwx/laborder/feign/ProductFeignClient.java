package com.liwx.laborder.feign;
import com.liwx.labcommon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "lab-product")
public interface ProductFeignClient {
    @GetMapping("/api/products/{id}")
    Result<?> getProduct(@PathVariable("id") Long id);
    @PutMapping("/api/products/{id}/deduct")
    Result<?> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}