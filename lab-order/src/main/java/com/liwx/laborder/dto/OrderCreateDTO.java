package com.liwx.laborder.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCreateDTO {
    @NotNull(message = "商品ID不能为空") private Long productId;
    @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为1") private Integer quantity;
}