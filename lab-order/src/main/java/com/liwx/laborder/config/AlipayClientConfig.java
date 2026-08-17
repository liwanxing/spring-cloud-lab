package com.liwx.laborder.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AlipayClientConfig {

    /** 仅在 payment.channel=alipay 时创建客户端 */
    @Bean
    @ConditionalOnProperty(name = "payment.channel", havingValue = "alipay")
    public AlipayClient alipayClient(AlipayProperties props) {
        return new DefaultAlipayClient(
                props.getGatewayUrl(),
                props.getAppId(),
                props.getMerchantPrivateKey(),
                "json", "UTF-8",
                props.getAlipayPublicKey(),
                "RSA2");
    }
}
