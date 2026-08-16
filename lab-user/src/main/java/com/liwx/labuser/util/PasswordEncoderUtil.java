package com.liwx.labuser.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordEncoderUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordEncoderUtil() {}

    /**
     * 单向：能验证，不能还原。验证密码对不对可以，从密文算出密码不行
     * 自带盐（Salt）：同样的密码，存出来永远不同。
     *
     * $2a$10$3jRFz.xlWGU83GgHkwz32u3I6XyEm1pz5iz9RXba.eg/hsJxHfgo6
     *  │   │  │                      │
     *  │   │  └── 22位盐              └── 31位哈希结果
     *  │   └───── 成本轮数（10 = 1024轮）
     *  └───────── 算法版本
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    // 盐和轮数直接存在密文里。所以只需要这一个参数——盐从密文里自己拆出来，不用单独存一列。
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
