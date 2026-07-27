package com.aurora.starter.verification.support;

import java.security.SecureRandom;

/**
 * 数字验证码生成器。
 */
public class VerificationCodeGenerator {

    private static final int RADIX = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成保留前导零的数字验证码。
     *
     * @param length 验证码长度
     * @return 数字验证码
     */
    public String generate(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(RADIX));
        }
        return code.toString();
    }
}
