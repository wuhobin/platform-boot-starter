package com.aurora.starter.verification.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.cache.impl.LocalCacheStore;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.ResourceProviders;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.spring.autoconfiguration.SpringImageCaptchaProperties;
import com.aurora.starter.verification.image.DefaultImageVerificationService;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.resource.CachingUrlResourceProvider;
import com.aurora.starter.verification.resource.DefaultImageVerificationResourceStore;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Locale;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * 图片行为验证码自动配置。
 */
@AutoConfiguration(beforeName = {
        "cloud.tianai.captcha.spring.autoconfiguration.CacheStoreAutoConfiguration",
        "cloud.tianai.captcha.spring.autoconfiguration.ImageCaptchaAutoConfiguration"
})
@ConditionalOnClass(ImageCaptchaApplication.class)
@ConditionalOnProperty(
        prefix = "platform.verification.image",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class ImageVerificationAutoConfiguration {

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    static BeanPostProcessor tianaiCaptchaPropertiesDefaultsPostProcessor(Environment environment) {
        return new TianaiCaptchaPropertiesDefaultsPostProcessor(environment);
    }

    @Bean
    @ConditionalOnMissingBean(ResourceStore.class)
    public ResourceStore imageVerificationResourceStore() {
        return new DefaultImageVerificationResourceStore();
    }

    @Bean
    @ConditionalOnMissingBean(ImageCaptchaResourceManager.class)
    public ImageCaptchaResourceManager imageCaptchaResourceManager(ResourceStore resourceStore) {
        ResourceProviders providers = new ResourceProviders();
        providers.registerResourceProvider(new CachingUrlResourceProvider());
        return new DefaultImageCaptchaResourceManager(resourceStore, providers);
    }

    @Bean
    @ConditionalOnMissingBean(ImageVerificationService.class)
    public ImageVerificationService imageVerificationService(
            ImageCaptchaApplication application,
            CacheStore cacheStore,
            ResourceStore resourceStore,
            SpringImageCaptchaProperties tianaiProperties,
            VerificationProperties properties) {
        String type = properties.getImage().getType().trim().toUpperCase(Locale.ROOT);
        validateConfiguration(type, cacheStore, resourceStore, tianaiProperties);
        return new DefaultImageVerificationService(application, type);
    }

    private void validateConfiguration(
            String type,
            CacheStore cacheStore,
            ResourceStore resourceStore,
            SpringImageCaptchaProperties properties) {
        if (cacheStore instanceof LocalCacheStore) {
            throw new IllegalStateException(
                    "Redis CacheStore is required when platform.verification.image.enabled=true");
        }
        if (!StringUtils.hasText(properties.getPrefix())) {
            throw new IllegalStateException("captcha.prefix must not be blank");
        }
        if (properties.getSecondary() == null
                || !Boolean.TRUE.equals(properties.getSecondary().getEnabled())) {
            throw new IllegalStateException(
                    "captcha.secondary.enabled must be true when image verification is enabled");
        }
        if (!StringUtils.hasText(properties.getSecondary().getKeyPrefix())) {
            throw new IllegalStateException("captcha.secondary.key-prefix must not be blank");
        }
        if (properties.getSecondary().getExpire() == null
                || properties.getSecondary().getExpire() <= 0) {
            throw new IllegalStateException("captcha.secondary.expire must be positive");
        }
        Long challengeExpire = properties.getExpire().getOrDefault(
                type, properties.getExpire().get("default"));
        if (challengeExpire == null || challengeExpire <= 0) {
            throw new IllegalStateException("captcha challenge expire must be positive");
        }
        if (resourceStore instanceof DefaultImageVerificationResourceStore
                && !Boolean.TRUE.equals(properties.getInitDefaultResource())) {
            throw new IllegalStateException(
                    "captcha.init-default-resource must be true when using the built-in ResourceStore");
        }
    }
}
