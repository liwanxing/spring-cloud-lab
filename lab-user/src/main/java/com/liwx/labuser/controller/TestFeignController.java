package com.liwx.labuser.controller;
import com.liwx.labcommon.common.Result;
import com.liwx.labuser.feign.ProductFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestFeignController {
    private final ProductFeignClient productFeignClient;

    @PostMapping("/deduct")
    public Result<?> testDeduct(@RequestParam Long productId, @RequestParam int quantity) {
        return productFeignClient.deductStock(productId, quantity);
    }
}