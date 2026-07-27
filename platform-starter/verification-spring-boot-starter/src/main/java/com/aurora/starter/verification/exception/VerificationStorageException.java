package com.aurora.starter.verification.exception;

/**
 * 验证码 Redis 操作失败。
 */
public class VerificationStorageException extends VerificationException {

    public VerificationStorageException(String message) {
        super(message);
    }

    public VerificationStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
