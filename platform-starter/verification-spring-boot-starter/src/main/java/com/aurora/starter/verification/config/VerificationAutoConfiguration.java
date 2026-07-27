package com.aurora.starter.verification.config;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.mail.DefaultMailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

/**
 * 验证码自动配置。
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "com.aurora.starter.redis.config.RedisAutoConfig"
})
@ConditionalOnProperty(
        prefix = "platform.verification.mail",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationAutoConfiguration {

    @Bean
    public VerificationCodeGenerator verificationCodeGenerator() {
        return new VerificationCodeGenerator();
    }

    @Bean
    public RedisMailVerificationRepository redisMailVerificationRepository(
            RedisCache redisCache,
            VerificationProperties properties) {
        return new RedisMailVerificationRepository(redisCache, properties);
    }

    @Bean
    @ConditionalOnMissingBean(MailVerificationService.class)
    public MailVerificationService mailVerificationService(
            JavaMailSender mailSender,
            RedisMailVerificationRepository repository,
            VerificationCodeGenerator codeGenerator,
            VerificationProperties properties,
            ObjectProvider<org.springframework.boot.autoconfigure.mail.MailProperties> mailPropertiesProvider) {
        String from = properties.getMail().getFrom();
        if (!StringUtils.hasText(from)) {
            org.springframework.boot.autoconfigure.mail.MailProperties mailProperties =
                    mailPropertiesProvider.getIfAvailable();
            from = mailProperties == null ? null : mailProperties.getUsername();
        }
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException(
                    "Either platform.verification.mail.from or spring.mail.username must be configured");
        }
        return new DefaultMailVerificationService(
                mailSender, repository, codeGenerator, properties, from.trim());
    }
}
