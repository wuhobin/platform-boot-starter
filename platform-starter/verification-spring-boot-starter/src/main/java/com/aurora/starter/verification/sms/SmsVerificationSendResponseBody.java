package com.aurora.starter.verification.sms;

/**
 * 阿里云发送短信验证码响应体。
 *
 * @param success            请求是否成功
 * @param code               响应码
 * @param message            响应消息
 * @param requestId          请求 ID
 * @param accessDeniedDetail 访问拒绝详情
 * @param model              发送结果
 */
public record SmsVerificationSendResponseBody(
        Boolean success,
        String code,
        String message,
        String requestId,
        String accessDeniedDetail,
        SmsVerificationSendResponseModel model) {
}
