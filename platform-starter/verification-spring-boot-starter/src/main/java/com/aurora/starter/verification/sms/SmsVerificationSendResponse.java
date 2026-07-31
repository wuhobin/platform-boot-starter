package com.aurora.starter.verification.sms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云发送短信验证码响应，不包含服务端返回的验证码。
 *
 * @param headers    HTTP 响应头
 * @param statusCode HTTP 状态码
 * @param body       响应体
 */
public record SmsVerificationSendResponse(
        Map<String, String> headers,
        Integer statusCode,
        SmsVerificationSendResponseBody body) {

    public SmsVerificationSendResponse {
        headers = headers == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }
}
