package com.aurora.starter.verification.mail;

import com.aurora.starter.verification.scene.VerificationScene;

/**
 * 邮件验证码校验请求。
 *
 * @param email 收件人邮箱
 * @param scene 业务场景
 * @param code  待校验验证码
 */
public record MailVerificationVerifyRequest(
        String email,
        VerificationScene scene,
        String code) {
}
