package com.liwx.laborder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("payments")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 支付单号（商户侧，传给支付宝的 out_trade_no） */
    private String payNo;
    private Long orderId;
    private String orderNo;
    /** 支付宝交易号，支付成功后回填 */
    private String tradeNo;
    /** 支付渠道：ALIPAY / MOCK */
    private String channel;
    private BigDecimal amount;
    /** PAYING / SUCCESS / FAILED / CLOSED（超时关单） */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
