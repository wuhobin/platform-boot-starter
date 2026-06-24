package com.aurora.starter.redis.exception;

/**
 * 限流异常.
 *
 * @author whb
 * @date 2026/6/18
 */
public class RateLimiterException extends RuntimeException {

    public RateLimiterException() {
        super("请求太频繁，请稍后再试");
    }

    public RateLimiterException(final String message) {
        super(message);
    }
}
