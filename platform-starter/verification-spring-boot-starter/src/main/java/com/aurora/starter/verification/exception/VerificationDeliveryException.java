package com.aurora.starter.verification.exception;

/**
 * 验证码投递失败。
 */
public class VerificationDeliveryException extends VerificationException {

    public VerificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
