package com.aurora.starter.verification.exception;

import com.aurora.starter.verification.sms.SmsVerificationSendResponse;

/**
 * 阿里云短信验证码投递失败。
 */
public class SmsVerificationDeliveryException extends VerificationDeliveryException {

    private final SmsVerificationSendResponse response;

    public SmsVerificationDeliveryException(
            String message,
            Throwable cause,
            SmsVerificationSendResponse response) {
        super(message, cause);
        this.response = response;
    }

    /**
     * 返回阿里云明确响应；网络异常等结果未知场景下为 {@code null}。
     *
     * @return 自定义短信响应
     */
    public SmsVerificationSendResponse getResponse() {
        return response;
    }
}
