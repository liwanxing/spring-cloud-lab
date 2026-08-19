package com.liwx.labuser.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * B阶段：让 @SaCheckRole 注解真正生效 —— sa-token 的注解鉴权依赖 SaInterceptor 扫描，
 * 此前全项目从未注册过拦截器，UserController 上的 @SaCheckRole("admin") 一直是摆设。
 * 默认即注解模式：只拦标注了 @SaCheck* 的方法/类，其余接口不设卡（登录校验已上移网关）。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
