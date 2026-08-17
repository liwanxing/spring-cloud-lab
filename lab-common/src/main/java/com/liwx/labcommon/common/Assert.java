package com.liwx.labcommon.common;
import com.liwx.labcommon.exception.BusinessException;
public final class Assert {
    private Assert() {}
    public static void notNull(Object o, String m) { if (o == null) throw new BusinessException(m); }
    public static void isTrue(boolean c, String m) { if (!c) throw new BusinessException(m); }
    public static void isFalse(boolean c, String m) { if (c) throw new BusinessException(m); }
}