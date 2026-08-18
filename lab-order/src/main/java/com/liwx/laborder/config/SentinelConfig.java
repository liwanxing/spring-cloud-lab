package com.liwx.laborder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liwx.labcommon.common.Result;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 限流统一响应：URL 资源被限流时默认返回纯文本 "Blocked by Sentinel"，
 * 这里改为项目统一的 Result JSON（前端能弹友好提示而不是拿到怪字符串）。
 */
@Configuration
public class SentinelConfig {

    @Bean
    public BlockExceptionHandler blockExceptionHandler(ObjectMapper objectMapper) {
        return (request, response, e) -> {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.error(429, "请求过于频繁，请稍后再试")));
        };
    }
}
