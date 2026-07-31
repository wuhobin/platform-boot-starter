package com.aurora.starter.verification.sms;

import com.aurora.starter.verification.scene.VerificationScene;

/**
 * 短信验证码校验请求。
 *
 * @param phoneNumber 中国大陆手机号，支持 11 位、86 或 +86 前缀
 * @param scene       业务场景
 * @param code        待校验验证码
 */
public record SmsVerificationVerifyRequest(
        String phoneNumber,
        VerificationScene scene,
        String code) {
}
