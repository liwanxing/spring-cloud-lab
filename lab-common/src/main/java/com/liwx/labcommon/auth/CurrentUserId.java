package com.liwx.labcommon.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * B阶段：当前登录用户 id —— 网关验明 token 后经 X-User-Id 头下发，
 * 由 CurrentUserIdResolver 自动注入参数，业务服务不再自己查会话。
 * 用法：public Result<?> xxx(@CurrentUserId Long userId, ...)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
