package com.liwx.laborder.dto;
import jakarta.validation.constraints.*;
import lombok.*;
/**
 * 下单商品项：刻意不含价格字段。
 * 前端展示的价格不可信（可被篡改），金额一律由服务端实时查询商品现价计算，
 * 并在下单时快照进订单明细。勿为"方便校验"而增加 price 字段。
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemCreateDTO {
    @NotNull(message = "商品ID不能为空") private Long productId;
    @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为1") private Integer quantity;
}
