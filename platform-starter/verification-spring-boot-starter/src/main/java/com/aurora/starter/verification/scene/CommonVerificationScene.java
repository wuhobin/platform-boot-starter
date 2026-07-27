package com.aurora.starter.verification.scene;

/**
 * 常用验证码业务场景。
 */
public enum CommonVerificationScene implements VerificationScene {

    REGISTER,
    LOGIN,
    RESET_PASSWORD,
    CHANGE_EMAIL;

    @Override
    public String code() {
        return name();
    }
}
