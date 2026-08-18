package com.liwx.laborder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置集，绑定 application.yaml 的 payment.alipay 前缀。
 *
 * 抽取原因：配置有两个消费方（AlipayClientConfig 建客户端、AlipayPaymentServiceImpl 业务用 notifyUrl/公钥验签），
 * 若用 @Value 则同一个 key 要在两个类里各写一遍，集中绑定为对象后注入同一个 bean 即可。
 */
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
