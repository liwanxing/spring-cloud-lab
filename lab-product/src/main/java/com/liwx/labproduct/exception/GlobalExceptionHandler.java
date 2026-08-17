package com.liwx.labproduct.exception;
import com.liwx.labproduct.common.Result;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException e) { return Result.error(e.getMessage()); }
    @ExceptionHandler(NotLoginException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) { return Result.error("未登录或登录已过期"); }
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return Result.error(msg);
    }
    @ExceptionHandler(DuplicateKeyException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicate(DuplicateKeyException e) { return Result.error("数据已存在"); }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) { log.error("系统异常", e); return Result.error("系统内部错误"); }
}