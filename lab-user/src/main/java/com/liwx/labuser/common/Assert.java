package com.liwx.labuser.common;

import com.liwx.labuser.exception.BusinessException;

public final class Assert {
    private Assert() {}
    public static void notNull(Object obj, String message) {
        if (obj == null) throw new BusinessException(message);
    }
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) throw new BusinessException(message);
    }
    public static void isTrue(boolean condition, String message) {
        if (!condition) throw new BusinessException(message);
    }
    public static void isFalse(boolean condition, String message) {
        if (condition) throw new BusinessException(message);
    }
}
