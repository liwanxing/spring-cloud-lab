package com.liwx.laborder.feign;

import com.liwx.labcommon.common.Result;
import org.springframework.stereotype.Component;

/**
 * ProductFeignClient 的 Sentinel 降级兜底：lab-product 不可用/熔断时走这里。
 * 关键约定：所有方法返回错误码（503），绝不返回成功 —— 调用方以 code 判定成败，
 * 假成功会导致下单漏扣库存、关单漏回补（Seata 层面因无异常也不会回滚）。
 */
@Component
public class ProductFeignFallback implements ProductFeignClient {

    @Override
    public Result<?> getProduct(Long id) {
        return Result.error(503, "商品服务繁忙，请稍后重试");
    }

    @Override
    public Result<?> deductStock(Long id, int quantity) {
        return Result.error(503, "商品服务繁忙，扣减失败");
    }

    @Override
    public Result<?> restoreStock(Long id, int quantity) {
        return Result.error(503, "商品服务繁忙，回补失败");
    }
}
