package com.liwx.laborder.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentVO {
    private String payNo;
    /** 支付跳转链接（支付宝收银台），mock 渠道为 null */
    private String payUrl;
    /** PAYING / SUCCESS / FAILED */
    private String status;
}
