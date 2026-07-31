package com.aurora.starter.verification.exception;

/**
 * 验证码发送配额类型。
 */
public enum VerificationRateLimitType {

    /**
     * 首次发送起滚动一小时配额。
     */
    HOURLY,

    /**
     * 北京时间自然日配额。
     */
    DAILY
}
