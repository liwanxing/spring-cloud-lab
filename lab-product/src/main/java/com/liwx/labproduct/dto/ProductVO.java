package com.liwx.labproduct.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVO {
    private Long id; private String name; private String description;
    private BigDecimal price; private Integer stock; private String category;
    private Integer status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
