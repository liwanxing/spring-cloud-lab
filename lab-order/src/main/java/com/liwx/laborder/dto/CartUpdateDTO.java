package com.liwx.laborder.dto;
import jakarta.validation.constraints.Min;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CartUpdateDTO {
    @Min(value = 1, message = "数量至少为1") private Integer quantity;
}