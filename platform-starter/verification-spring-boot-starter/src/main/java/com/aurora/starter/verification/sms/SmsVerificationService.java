package com.aurora.starter.verification.sms;

/**
 * 短信验证码服务。
 */
public interface SmsVerificationService {

    /**
     * 本地生成验证码并通过阿里云同步发送。
     *
     * @param request 发送请求
     * @return 不含验证码字段的阿里云响应映射
     */
    SmsVerificationSendResponse send(SmsVerificationSendRequest request);

    /**
     * 在本地校验并原子消费验证码。
     *
     * @param request 校验请求
     * @return 验证码正确且消费成功时返回 {@code true}
     */
    boolean verifyAndConsume(SmsVerificationVerifyRequest request);
}
