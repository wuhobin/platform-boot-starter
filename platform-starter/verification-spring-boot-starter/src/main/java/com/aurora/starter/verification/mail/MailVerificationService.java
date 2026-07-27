package com.aurora.starter.verification.mail;

/**
 * 邮件验证码服务。
 */
public interface MailVerificationService {

    /**
     * 生成并同步发送验证码邮件。
     *
     * @param request 发送请求
     */
    void send(MailVerificationSendRequest request);

    /**
     * 校验并原子消费验证码。
     *
     * @param request 校验请求
     * @return 验证码正确且消费成功时返回 {@code true}
     */
    boolean verifyAndConsume(MailVerificationVerifyRequest request);
}
