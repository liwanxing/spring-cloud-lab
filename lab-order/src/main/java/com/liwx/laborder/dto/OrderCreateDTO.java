package com.liwx.laborder.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCreateDTO {
    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemCreateDTO> items;
}
