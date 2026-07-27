package com.aurora.starter.verification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

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
}
