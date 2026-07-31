package com.aurora.starter.verification.sms;

import com.aurora.starter.verification.scene.VerificationScene;

/**
 * 短信验证码发送请求。
 *
 * @param phoneNumber 中国大陆手机号，支持 11 位、86 或 +86 前缀
 * @param scene       业务场景
 */
public record SmsVerificationSendRequest(
        String phoneNumber,
        VerificationScene scene) {
}
