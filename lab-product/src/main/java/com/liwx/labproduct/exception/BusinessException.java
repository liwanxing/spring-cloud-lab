package com.liwx.labproduct.exception;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(String msg) { super(msg); this.code = 400; }
}