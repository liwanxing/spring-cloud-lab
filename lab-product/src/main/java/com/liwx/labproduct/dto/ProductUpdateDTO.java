package com.liwx.labproduct.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductUpdateDTO {
    private String name;
    private String description;
    @DecimalMin(value = "0.01", message = "价格必须大于0") private BigDecimal price;
    @Min(value = 0, message = "库存不能为负数") private Integer stock;
    private String category;
    private Integer status;
}
