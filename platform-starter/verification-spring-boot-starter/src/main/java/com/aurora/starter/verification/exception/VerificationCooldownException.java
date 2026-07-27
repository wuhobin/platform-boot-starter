package com.aurora.starter.verification.exception;

import java.time.Duration;

/**
 * 验证码发送过于频繁。
 */
public class VerificationCooldownException extends VerificationException {

    private final Duration retryAfter;

    public VerificationCooldownException(Duration retryAfter) {
        super("Verification code was requested too frequently, retry after " + retryAfter.toMillis() + " ms");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
