package com.aurora.starter.verification.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * 验证码配置。
 */
@Data
@Validated
@ConfigurationProperties(prefix = VerificationProperties.PREFIX)
public class VerificationProperties {

    public static final String PREFIX = "platform.verification";

    /**
     * Redis Key 前缀。
     */
    @NotBlank
    private String keyPrefix = "verification";

    @Valid
    @NotNull
    private MailProperties mail = new MailProperties();

    @Valid
    @NotNull
    private ImageProperties image = new ImageProperties();

    @Valid
    @NotNull
    private SmsProperties sms = new SmsProperties();

    /**
     * 邮件验证码配置。
     */
    @Data
    public static class MailProperties {

        /**
         * 是否启用邮件验证码。
         */
        private boolean enabled;

        /**
         * 发件人邮箱，为空时使用 spring.mail.username。
         */
        @Email
        private String from;

        /**
         * 发件人显示名称。
         */
        private String fromName;

        /**
         * 验证码长度。
         */
        @Min(4)
        @Max(8)
        private int codeLength = 6;

        /**
         * 验证码有效期。
         */
        @NotNull
        private Duration expireTime = Duration.ofMinutes(5);

        /**
         * 同一邮箱和场景的发送冷却时间。
         */
        @NotNull
        private Duration cooldown = Duration.ofSeconds(60);

        @AssertTrue(message = "expire-time must be between 30 seconds and 30 minutes")
        public boolean isExpireTimeValid() {
            return expireTime != null
                    && expireTime.compareTo(Duration.ofSeconds(30)) >= 0
                    && expireTime.compareTo(Duration.ofMinutes(30)) <= 0;
        }

        @AssertTrue(message = "cooldown must be positive")
        public boolean isCooldownValid() {
            return cooldown != null && !cooldown.isNegative() && !cooldown.isZero();
        }
    }

    /**
     * 短信验证码配置。
     */
    @Data
    public static class SmsProperties {

        /**
         * 是否启用短信验证码。
         */
        private boolean enabled;

        /**
         * 阿里云 AccessKey ID。
         */
        @ToString.Exclude
        private String accessKeyId;

        /**
         * 阿里云 AccessKey Secret。
         */
        @ToString.Exclude
        private String accessKeySecret;

        /**
         * 验证码有效期。
         */
        @NotNull
        private Duration expireTime = Duration.ofMinutes(5);

        /**
         * 同一手机号和场景的发送冷却时间。
         */
        @NotNull
        private Duration cooldown = Duration.ofSeconds(60);

        /**
         * 同一手机号一小时内的最大发送次数，跨场景累计。
         */
        @Min(1)
        private int hourlyLimit = 5;

        /**
         * 同一手机号一个自然日内的最大发送次数，跨场景累计。
         */
        @Min(1)
        private int dailyLimit = 10;

        /**
         * 单个验证码允许的最大错误次数。
         */
        @Min(1)
        @Max(10)
        private int maxFailedAttempts = 5;

        @AssertTrue(message = "access-key-id and access-key-secret must be configured when SMS verification is enabled")
        public boolean isCredentialsValid() {
            return !enabled
                    || (StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret));
        }

        @AssertTrue(message = "expire-time must be between 30 seconds and 30 minutes")
        public boolean isExpireTimeValid() {
            return expireTime != null
                    && expireTime.compareTo(Duration.ofSeconds(30)) >= 0
                    && expireTime.compareTo(Duration.ofMinutes(30)) <= 0;
        }

        @AssertTrue(message = "cooldown must be positive")
        public boolean isCooldownValid() {
            return cooldown != null && !cooldown.isNegative() && !cooldown.isZero();
        }

        @AssertTrue(message = "daily-limit must be greater than or equal to hourly-limit")
        public boolean isLimitsValid() {
            return dailyLimit >= hourlyLimit;
        }
    }

    /**
     * 图片验证码配置。
     */
    @Data
    public static class ImageProperties {

        private static final Set<String> SUPPORTED_TYPES = Set.of(
                CaptchaTypeConstant.SLIDER,
                CaptchaTypeConstant.ROTATE,
                CaptchaTypeConstant.CONCAT,
                CaptchaTypeConstant.WORD_IMAGE_CLICK);

        /**
         * 是否启用图片验证码平台服务。
         */
        private boolean enabled;

        /**
         * tianai-captcha 验证码类型。
         */
        @NotBlank
        private String type = CaptchaTypeConstant.SLIDER;

        @AssertTrue(message = "type must be one of SLIDER, ROTATE, CONCAT, WORD_IMAGE_CLICK")
        public boolean isTypeSupported() {
            return type != null && SUPPORTED_TYPES.contains(type.trim().toUpperCase(Locale.ROOT));
        }
    }
}
