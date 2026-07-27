package com.aurora.starter.verification.config;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.mail.MailVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VerificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(RedisCache.class, () -> mock(RedisCache.class));

    @Test
    void isDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(MailVerificationService.class);
            assertThat(context).doesNotHaveBean(VerificationProperties.class);
        });
    }

    @Test
    void createsServiceWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.verification.mail.enabled=true",
                        "platform.verification.mail.from=no-reply@example.com",
                        "platform.verification.mail.code-length=8",
                        "platform.verification.mail.expire-time=10m",
                        "platform.verification.mail.cooldown=30s")
                .run(context -> {
                    assertThat(context).hasSingleBean(MailVerificationService.class);
                    VerificationProperties properties = context.getBean(VerificationProperties.class);
                    assertThat(properties.getMail().getCodeLength()).isEqualTo(8);
                    assertThat(properties.getMail().getExpireTime()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.getMail().getCooldown()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void fallsBackToSpringMailUsernameWhenFromIsMissing() {
        org.springframework.boot.autoconfigure.mail.MailProperties mailProperties =
                new org.springframework.boot.autoconfigure.mail.MailProperties();
        mailProperties.setUsername("smtp-account@example.com");

        contextRunner
                .withBean(
                        org.springframework.boot.autoconfigure.mail.MailProperties.class,
                        () -> mailProperties)
                .withPropertyValues("platform.verification.mail.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(MailVerificationService.class);
                    assertThat(org.springframework.test.util.ReflectionTestUtils.getField(
                            context.getBean(MailVerificationService.class), "from"))
                            .isEqualTo("smtp-account@example.com");
                });
    }

    @Test
    void backsOffForCustomService() {
        MailVerificationService customService = mock(MailVerificationService.class);
        contextRunner
                .withBean(MailVerificationService.class, () -> customService)
                .withPropertyValues(
                        "platform.verification.mail.enabled=true",
                        "platform.verification.mail.from=no-reply@example.com")
                .run(context -> assertThat(context.getBean(MailVerificationService.class))
                        .isSameAs(customService));
    }

    @Test
    void failsFastWhenFromIsMissing() {
        contextRunner
                .withPropertyValues("platform.verification.mail.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsExpireTimeOutsideConfiguredRange() {
        contextRunner
                .withPropertyValues(
                        "platform.verification.mail.enabled=true",
                        "platform.verification.mail.from=no-reply@example.com",
                        "platform.verification.mail.expire-time=10s")
                .run(context -> assertThat(context).hasFailed());
    }
}
