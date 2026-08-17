package com.liwx.laborder.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder @TableName("cart")
public class Cart {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}