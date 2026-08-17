package com.liwx.laborder.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder @TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    @TableLogic private Integer deleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}