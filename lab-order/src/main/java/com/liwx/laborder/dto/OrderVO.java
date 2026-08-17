package com.liwx.laborder.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderVO {
    private Long id; private String orderNo; private Long userId;
    private Long productId; private String productName; private BigDecimal productPrice;
    private Integer quantity; private BigDecimal totalAmount; private String status;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}