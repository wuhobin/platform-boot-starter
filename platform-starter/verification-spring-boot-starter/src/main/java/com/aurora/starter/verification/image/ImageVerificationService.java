package com.aurora.starter.verification.image;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;

/**
 * 图片行为验证码服务。
 */
public interface ImageVerificationService {

    /**
     * 使用配置的固定类型生成图片验证码。
     *
     * @return 可直接交给 tianai Web SDK 的验证码数据
     */
    ImageCaptchaVO generate();

    /**
     * 校验行为轨迹。挑战数据无论成功或失败都只能匹配一次。
     *
     * @param captchaId 验证码 ID
     * @param track     行为轨迹
     * @return 轨迹匹配成功时返回 {@code true}
     */
    boolean match(String captchaId, ImageCaptchaTrack track);

    /**
     * 原子校验并消费二次验证凭证。
     *
     * @param captchaId 轨迹匹配成功后的验证码 ID
     * @return 凭证有效且消费成功时返回 {@code true}
     */
    boolean verifyAndConsume(String captchaId);
}
