package com.liwx.laborder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment.alipay")
public class AlipayProperties {
    /** 沙箱 APPID */
    private String appId;
    /** 沙箱网关：https://openapi-sandbox.dl.alipaydev.com/gateway.do */
    private String gatewayUrl;
    /** 应用私钥（密钥工具生成） */
    private String merchantPrivateKey;
    /** 支付宝公钥（平台配置公钥后回显） */
    private String alipayPublicKey;
    /** 异步通知地址，需公网可达（内网穿透域名），留空则不设置、依赖主动查单 */
    private String notifyUrl;
    /** 支付完成后浏览器跳转地址 */
    private String returnUrl;
}
