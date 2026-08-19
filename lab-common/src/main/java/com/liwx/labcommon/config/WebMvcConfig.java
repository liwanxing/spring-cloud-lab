package com.liwx.labcommon.config;

import com.liwx.labcommon.auth.CurrentUserIdResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * B阶段：注册 @CurrentUserId 参数解析器。
 * 业务服务 scanBasePackages="com.liwx" 扫到这里自动生效，无需各自配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdResolver());
    }
}
