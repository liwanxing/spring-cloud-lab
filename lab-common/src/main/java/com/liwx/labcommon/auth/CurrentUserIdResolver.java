package com.liwx.labcommon.auth;

import com.liwx.labcommon.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 取网关下发的 X-User-Id 头注入 @CurrentUserId 参数。
 * 头缺失说明请求绕过网关直连了服务（或网关未部署鉴权）—— 按"未登录"语义回 401，
 * 而不是放行一个无人验明的请求。
 */
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

    /** 与 lab-gateway AuthGlobalFilter.X_USER_ID 约定一致（lab-common 不被网关依赖，改一处记两处） */
    public static final String X_USER_ID = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && (parameter.getParameterType() == Long.class || parameter.getParameterType() == Long.TYPE);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(X_USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return Long.valueOf(userId);
    }
}
