package com.aurora.starter.verification.exception;

/**
 * 图片验证码生成、存储或校验基础设施异常。
 */
public class ImageVerificationException extends VerificationException {

    public ImageVerificationException(String message) {
        super(message);
    }

    public ImageVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
