package com.aurora.starter.verification.image;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.aurora.starter.verification.exception.ImageVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * 基于 tianai-captcha 的默认图片验证码服务。
 */
@Slf4j
public class DefaultImageVerificationService implements ImageVerificationService {

    private final ImageCaptchaApplication application;
    private final SecondaryVerificationApplication secondaryApplication;
    private final String type;

    public DefaultImageVerificationService(
            ImageCaptchaApplication application,
            String type) {
        this.application = Objects.requireNonNull(application, "application must not be null");
        if (!(application instanceof SecondaryVerificationApplication secondary)) {
            throw new IllegalStateException(
                    "tianai secondary verification must be enabled when image verification is enabled");
        }
        this.secondaryApplication = secondary;
        this.type = requireText(type, "type").toUpperCase(Locale.ROOT);
    }

    @Override
    public ImageCaptchaVO generate() {
        try {
            ApiResponse<ImageCaptchaVO> response = application.generateCaptcha(type);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                String message = response == null ? "empty response" : response.getMsg();
                throw new ImageVerificationException(
                        "Failed to generate image captcha: " + message);
            }
            ImageCaptchaVO captcha = response.getData();
            log.debug("Image captcha generated, captchaId={}, type={}", captcha.getId(), type);
            log.info("Image captcha generation completed, type={}, success=true", type);
            return captcha;
        } catch (ImageVerificationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Image captcha generation failed, type={}", type, ex);
            throw new ImageVerificationException("Failed to generate image captcha", ex);
        }
    }

    @Override
    public boolean match(String captchaId, ImageCaptchaTrack track) {
        String normalizedId = requireText(captchaId, "captchaId");
        if (track == null) {
            throw new IllegalArgumentException("track must not be null");
        }

        log.debug("Matching image captcha, captchaId={}, track={}", normalizedId, track);
        try {
            ApiResponse<?> response = application.matching(normalizedId, track);
            boolean matched = response != null && response.isSuccess();
            log.info("Image captcha matching completed, success={}", matched);
            return matched;
        } catch (IllegalArgumentException ex) {
            log.debug("Image captcha track rejected, captchaId={}, track={}",
                    normalizedId, track, ex);
            log.info("Image captcha matching completed, success=false");
            return false;
        } catch (RuntimeException ex) {
            log.error("Image captcha matching failed, captchaId={}, track={}",
                    normalizedId, track, ex);
            throw new ImageVerificationException("Failed to match image captcha", ex);
        }
    }

    @Override
    public boolean verifyAndConsume(String captchaId) {
        String normalizedId = requireText(captchaId, "captchaId");
        log.debug("Consuming image verification credential, captchaId={}", normalizedId);
        try {
            boolean verified = secondaryApplication.secondaryVerification(normalizedId);
            log.info("Image verification credential consumption completed, success={}", verified);
            return verified;
        } catch (RuntimeException ex) {
            log.error("Image verification credential consumption failed, captchaId={}",
                    normalizedId, ex);
            throw new ImageVerificationException(
                    "Failed to consume image verification credential", ex);
        }
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
