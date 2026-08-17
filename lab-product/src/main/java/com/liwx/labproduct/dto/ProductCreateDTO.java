package com.liwx.labproduct.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCreateDTO {
    @NotBlank(message = "商品名称不能为空") private String name;
    private String description;
    @NotNull @DecimalMin(value = "0.01", message = "价格必须大于0") private BigDecimal price;
    @NotNull @Min(value = 0, message = "库存不能为负数") private Integer stock;
    private String category;
}
