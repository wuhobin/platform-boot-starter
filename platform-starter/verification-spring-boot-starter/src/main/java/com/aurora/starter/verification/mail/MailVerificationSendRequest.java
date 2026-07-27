package com.aurora.starter.verification.mail;

import com.aurora.starter.verification.scene.VerificationScene;

/**
 * 邮件验证码发送请求。
 *
 * @param email       收件人邮箱，只允许单个地址
 * @param scene       业务场景
 * @param subject     邮件主题
 * @param content     邮件正文模板，必须包含 {@code {code}}
 * @param contentType 正文类型
 */
public record MailVerificationSendRequest(
        String email,
        VerificationScene scene,
        String subject,
        String content,
        MailContentType contentType) {
}
