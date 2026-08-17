package com.liwx.laborder.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String status;
    private List<OrderItemVO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
