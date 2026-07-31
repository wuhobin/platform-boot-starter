package com.aurora.starter.verification.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.redis.RedisSmsVerificationRepository;
import com.aurora.starter.verification.sms.SmsVerificationService;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SmsVerificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SmsVerificationAutoConfiguration.class))
            .withBean(RedisCache.class, () -> mock(RedisCache.class))
            .withBean(
                    SmsVerificationAutoConfiguration.CLIENT_BEAN_NAME,
                    Client.class,
                    () -> mock(Client.class));

    @Test
    void isDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SmsVerificationService.class);
            assertThat(context).doesNotHaveBean(VerificationProperties.class);
        });
    }

    @Test
    void createsServiceWithDocumentedDefaults() {
        contextRunner
                .withPropertyValues(
                        "platform.verification.sms.enabled=true",
                        "platform.verification.sms.access-key-id=test-id",
                        "platform.verification.sms.access-key-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                    assertThat(context).hasSingleBean(RedisSmsVerificationRepository.class);
                    assertThat(context).hasSingleBean(VerificationCodeGenerator.class);

                    VerificationProperties.SmsProperties sms = context
                            .getBean(VerificationProperties.class)
                            .getSms();
                    assertThat(sms.getExpireTime()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(sms.getCooldown()).isEqualTo(Duration.ofSeconds(60));
                    assertThat(sms.getHourlyLimit()).isEqualTo(5);
                    assertThat(sms.getDailyLimit()).isEqualTo(10);
                    assertThat(sms.getMaxFailedAttempts()).isEqualTo(5);
                    assertThat(sms.toString()).doesNotContain("test-id", "test-secret");
                });
    }

    @Test
    void bindsOptionalLimits() {
        contextRunner
                .withPropertyValues(
                        "platform.verification.sms.enabled=true",
                        "platform.verification.sms.access-key-id=test-id",
                        "platform.verification.sms.access-key-secret=test-secret",
                        "platform.verification.sms.expire-time=10m",
                        "platform.verification.sms.cooldown=30s",
                        "platform.verification.sms.hourly-limit=8",
                        "platform.verification.sms.daily-limit=20",
                        "platform.verification.sms.max-failed-attempts=7")
                .run(context -> {
                    VerificationProperties.SmsProperties sms = context
                            .getBean(VerificationProperties.class)
                            .getSms();
                    assertThat(sms.getExpireTime()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(sms.getCooldown()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(sms.getHourlyLimit()).isEqualTo(8);
                    assertThat(sms.getDailyLimit()).isEqualTo(20);
                    assertThat(sms.getMaxFailedAttempts()).isEqualTo(7);
                });
    }

    @Test
    void failsFastWhenCredentialsAreMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SmsVerificationAutoConfiguration.class))
                .withBean(RedisCache.class, () -> mock(RedisCache.class))
                .withPropertyValues("platform.verification.sms.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void createsAliyunClientFromStarterCredentials() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SmsVerificationAutoConfiguration.class))
                .withBean(RedisCache.class, () -> mock(RedisCache.class))
                .withPropertyValues(
                        "platform.verification.sms.enabled=true",
                        "platform.verification.sms.access-key-id=test-id",
                        "platform.verification.sms.access-key-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean(SmsVerificationAutoConfiguration.CLIENT_BEAN_NAME);
                    assertThat(context.getBean(SmsVerificationAutoConfiguration.CLIENT_BEAN_NAME))
                            .isInstanceOf(Client.class);
                });
    }

    @Test
    void rejectsInvalidLimits() {
        contextRunner
                .withPropertyValues(
                        "platform.verification.sms.enabled=true",
                        "platform.verification.sms.access-key-id=test-id",
                        "platform.verification.sms.access-key-secret=test-secret",
                        "platform.verification.sms.hourly-limit=11",
                        "platform.verification.sms.daily-limit=10")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void backsOffForCustomServiceAndRepository() {
        SmsVerificationService customService = mock(SmsVerificationService.class);
        RedisSmsVerificationRepository customRepository = mock(RedisSmsVerificationRepository.class);

        contextRunner
                .withBean(SmsVerificationService.class, () -> customService)
                .withBean(RedisSmsVerificationRepository.class, () -> customRepository)
                .withPropertyValues(
                        "platform.verification.sms.enabled=true",
                        "platform.verification.sms.access-key-id=test-id",
                        "platform.verification.sms.access-key-secret=test-secret")
                .run(context -> {
                    assertThat(context.getBean(SmsVerificationService.class)).isSameAs(customService);
                    assertThat(context.getBean(RedisSmsVerificationRepository.class))
                            .isSameAs(customRepository);
                });
    }
}
