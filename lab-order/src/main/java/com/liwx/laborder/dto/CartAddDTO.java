package com.liwx.laborder.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CartAddDTO {
    @NotNull(message = "商品ID不能为空") private Long productId;
    @Min(value = 1, message = "数量至少为1") private Integer quantity = 1;
}