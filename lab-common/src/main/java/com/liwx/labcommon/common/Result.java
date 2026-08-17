package com.liwx.labcommon.common;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class Result<T> {
    private int code; private String message; private T data;
    public static <T> Result<T> success(T data) { return new Result<>(200, "success", data); }
    public static <T> Result<T> error(int code, String msg) { return new Result<>(code, msg, null); }
    public static <T> Result<T> error(String msg) { return new Result<>(500, msg, null); }
}