package com.aurora.starter.verification.sms;

/**
 * 阿里云发送短信验证码业务结果。
 *
 * @param requestId 请求 ID
 * @param outId     外部流水号
 * @param bizId     阿里云发送流水号
 */
public record SmsVerificationSendResponseModel(
        String requestId,
        String outId,
        String bizId) {
}
