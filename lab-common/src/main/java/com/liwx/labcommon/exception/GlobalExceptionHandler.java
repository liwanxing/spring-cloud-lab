package com.liwx.labcommon.exception;
import com.liwx.labcommon.common.Result;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler {
    /** 业务 code 落在合法 HTTP 状态区间（400-599）时如实透传到 HTTP 状态码（如降级 503），否则归 400 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletResponse response) {
        response.setStatus(httpStatusOf(e.getCode()));
        return Result.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(NotLoginException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) { return Result.error(401, "未登录或登录已过期"); }
    @ExceptionHandler(NotRoleException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRole(NotRoleException e) { return Result.error(403, "无访问权限"); }
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }
    @ExceptionHandler(DuplicateKeyException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicate(DuplicateKeyException e) { return Result.error(409, "数据已存在"); }
    /**
     * 兜底分支。注：Seata 2.0 会把 @GlobalTransactional 内抛出的异常包一层
     * RuntimeException("try to proceed invocation error")，BusinessException 类型丢失后落到这里；
     * 解包还原 400 语义（只影响响应形态，全局回滚不受影响）。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletResponse response) {
        if (e.getCause() instanceof BusinessException be) {
            response.setStatus(httpStatusOf(be.getCode()));
            return Result.error(be.getCode(), be.getMessage());
        }
        log.error("系统异常", e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Result.error("系统内部错误");
    }

    private static int httpStatusOf(int code) { return code >= 400 && code <= 599 ? code : 400; }
}