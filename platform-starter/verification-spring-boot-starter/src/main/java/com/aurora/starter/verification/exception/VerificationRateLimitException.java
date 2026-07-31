package com.aurora.starter.verification.exception;

import java.time.Duration;

/**
 * 验证码发送配额已耗尽。
 */
public class VerificationRateLimitException extends VerificationException {

    private final VerificationRateLimitType type;
    private final Duration retryAfter;

    public VerificationRateLimitException(VerificationRateLimitType type, Duration retryAfter) {
        super("Verification " + type.name().toLowerCase()
                + " rate limit exceeded, retry after " + retryAfter.toMillis() + " ms");
        this.type = type;
        this.retryAfter = retryAfter;
    }

    public VerificationRateLimitType getType() {
        return type;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
