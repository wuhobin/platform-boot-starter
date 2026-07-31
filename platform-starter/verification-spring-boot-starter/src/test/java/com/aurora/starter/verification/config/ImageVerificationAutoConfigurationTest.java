package com.aurora.starter.verification.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.cache.impl.LocalCacheStore;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.spring.autoconfiguration.CacheStoreAutoConfiguration;
import cloud.tianai.captcha.spring.autoconfiguration.ImageCaptchaAutoConfiguration;
import cloud.tianai.captcha.spring.autoconfiguration.SpringImageCaptchaProperties;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.spring.store.impl.RedisCacheStore;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.resource.DefaultImageVerificationResourceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ImageVerificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ImageVerificationAutoConfiguration.class))
            .withUserConfiguration(TianaiPropertiesConfiguration.class);

    @Test
    void isDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ImageVerificationService.class);
            assertThat(context).doesNotHaveBean(DefaultImageVerificationResourceStore.class);
            assertThat(context).doesNotHaveBean(VerificationProperties.class);
        });
    }

    @Test
    void createsServiceAndAppliesSafeTianaiDefaultsWhenEnabled() {
        enabledRunner(mock(CacheStore.class)).run(context -> {
            assertThat(context).hasSingleBean(ImageVerificationService.class);
            assertThat(context).hasSingleBean(ResourceStore.class);
            assertThat(context.getBean(ResourceStore.class))
                    .isInstanceOf(DefaultImageVerificationResourceStore.class);

            VerificationProperties platformProperties =
                    context.getBean(VerificationProperties.class);
            assertThat(platformProperties.getImage().isEnabled()).isTrue();
            assertThat(platformProperties.getImage().getType())
                    .isEqualTo(CaptchaTypeConstant.SLIDER);

            SpringImageCaptchaProperties tianaiProperties =
                    context.getBean(SpringImageCaptchaProperties.class);
            assertThat(tianaiProperties.getPrefix())
                    .isEqualTo("verification:image:challenge");
            assertThat(tianaiProperties.getExpire())
                    .containsEntry("default", 120_000L);
            assertThat(tianaiProperties.getInitDefaultResource()).isTrue();
            assertThat(tianaiProperties.getSecondary()).isNotNull();
            assertThat(tianaiProperties.getSecondary().getEnabled()).isTrue();
            assertThat(tianaiProperties.getSecondary().getExpire()).isEqualTo(60_000L);
            assertThat(tianaiProperties.getSecondary().getKeyPrefix())
                    .isEqualTo("verification:image:secondary");
        });
    }

    @Test
    void preservesExplicitTianaiOverrides() {
        enabledRunner(mock(CacheStore.class))
                .withBean(ResourceStore.class, () -> mock(ResourceStore.class))
                .withPropertyValues(
                        "captcha.prefix=custom:challenge",
                        "captcha.expire[default]=45000",
                        "captcha.initDefaultResource=false",
                        "captcha.secondary.enabled=true",
                        "captcha.secondary.expire=30000",
                        "captcha.secondary.keyPrefix=custom:secondary")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SpringImageCaptchaProperties properties =
                            context.getBean(SpringImageCaptchaProperties.class);
                    assertThat(properties.getPrefix()).isEqualTo("custom:challenge");
                    assertThat(properties.getExpire()).containsEntry("default", 45_000L);
                    assertThat(properties.getInitDefaultResource()).isFalse();
                    assertThat(properties.getSecondary().getExpire()).isEqualTo(30_000L);
                    assertThat(properties.getSecondary().getKeyPrefix())
                            .isEqualTo("custom:secondary");
                });
    }

    @Test
    void usesRelaxedPlatformKeyPrefixForTianaiDefaults() {
        enabledRunner(mock(CacheStore.class))
                .withPropertyValues("platform.verification.keyPrefix=App:Verification")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SpringImageCaptchaProperties properties =
                            context.getBean(SpringImageCaptchaProperties.class);
                    assertThat(properties.getPrefix())
                            .isEqualTo("app:verification:image:challenge");
                    assertThat(properties.getSecondary().getKeyPrefix())
                            .isEqualTo("app:verification:image:secondary");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"SLIDER", "ROTATE", "CONCAT", "WORD_IMAGE_CLICK"})
    void integratesWithTianaiAutoConfigurationsForEverySupportedType(String type) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ImageVerificationAutoConfiguration.class,
                        CacheStoreAutoConfiguration.class,
                        ImageCaptchaAutoConfiguration.class))
                .withPropertyValues(
                        "platform.verification.image.enabled=true",
                        "platform.verification.image.type=" + type)
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ImageVerificationService.class);
                    assertThat(context).hasSingleBean(ImageCaptchaApplication.class);
                    assertThat(context.getBean(ImageCaptchaApplication.class))
                            .isInstanceOf(SecondaryVerificationApplication.class);
                    assertThat(context.getBean(CacheStore.class))
                            .isInstanceOf(RedisCacheStore.class);
                    assertThat(context.getBean(ResourceStore.class))
                            .isInstanceOf(DefaultImageVerificationResourceStore.class);
                    DefaultImageVerificationResourceStore store =
                            (DefaultImageVerificationResourceStore) context.getBean(ResourceStore.class);
                    assertThat(store.listResourcesByTypeAndTag(type, "default")).hasSize(5);
                    assertThat(context.getBean(ImageCaptchaResourceManager.class))
                            .isInstanceOf(DefaultImageCaptchaResourceManager.class);
                });
    }

    @Test
    void backsOffForCustomService() {
        ImageVerificationService customService = mock(ImageVerificationService.class);

        contextRunner
                .withPropertyValues("platform.verification.image.enabled=true")
                .withBean(ImageVerificationService.class, () -> customService)
                .run(context -> assertThat(context.getBean(ImageVerificationService.class))
                        .isSameAs(customService));
    }

    @Test
    void backsOffForCustomResourceStore() {
        ResourceStore customStore = mock(ResourceStore.class);

        enabledRunner(mock(CacheStore.class))
                .withBean(ResourceStore.class, () -> customStore)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceStore.class);
                    assertThat(context.getBean(ResourceStore.class)).isSameAs(customStore);
                    assertThat(context)
                            .doesNotHaveBean(DefaultImageVerificationResourceStore.class);
                });
    }

    @Test
    void rejectsTianaiLocalCacheFallback() {
        enabledRunner(new LocalCacheStore()).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(
                            "Redis CacheStore is required when platform.verification.image.enabled=true");
        });
    }

    @Test
    void rejectsExplicitlyDisabledSecondaryVerification() {
        enabledRunner(mock(CacheStore.class))
                .withPropertyValues("captcha.secondary.enabled=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "captcha.secondary.enabled must be true when image verification is enabled");
                });
    }

    @Test
    void rejectsBlankTianaiKeyPrefixes() {
        enabledRunner(mock(CacheStore.class))
                .withPropertyValues("captcha.prefix=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("captcha.prefix must not be blank");
                });

        enabledRunner(mock(CacheStore.class))
                .withPropertyValues("captcha.secondary.key-prefix=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "captcha.secondary.key-prefix must not be blank");
                });
    }

    private ApplicationContextRunner enabledRunner(CacheStore cacheStore) {
        return contextRunner
                .withPropertyValues("platform.verification.image.enabled=true")
                .withBean(CacheStore.class, () -> cacheStore)
                .withBean(
                        ImageCaptchaApplication.class,
                        () -> mock(SecondaryVerificationApplication.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SpringImageCaptchaProperties.class)
    static class TianaiPropertiesConfiguration {
    }
}
