package com.liwx.labuser.feign;
import com.liwx.labcommon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "lab-product")
public interface ProductFeignClient {
    @PutMapping("/api/products/{id}/deduct")
    Result<?> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}