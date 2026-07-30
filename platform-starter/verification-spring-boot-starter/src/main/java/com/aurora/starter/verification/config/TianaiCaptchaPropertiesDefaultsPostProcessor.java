package com.aurora.starter.verification.config;

import cloud.tianai.captcha.spring.autoconfiguration.SecondaryVerificationProperties;
import cloud.tianai.captcha.spring.autoconfiguration.SpringImageCaptchaProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Locale;

/**
 * 在不覆盖用户显式配置的前提下，为 tianai-captcha 补充平台安全默认值。
 */
final class TianaiCaptchaPropertiesDefaultsPostProcessor implements BeanPostProcessor {

    private static final String CAPTCHA_PREFIX = "captcha.prefix";
    private static final String DEFAULT_EXPIRE = "captcha.expire.default";
    private static final String INIT_DEFAULT_RESOURCE = "captcha.init-default-resource";
    private static final String SECONDARY_ENABLED = "captcha.secondary.enabled";
    private static final String SECONDARY_EXPIRE = "captcha.secondary.expire";
    private static final String SECONDARY_KEY_PREFIX = "captcha.secondary.key-prefix";

    private static final long DEFAULT_CHALLENGE_EXPIRE_MILLIS = 120_000L;
    private static final long DEFAULT_SECONDARY_EXPIRE_MILLIS = 60_000L;

    private final Binder binder;

    TianaiCaptchaPropertiesDefaultsPostProcessor(Environment environment) {
        this.binder = Binder.get(environment);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof SpringImageCaptchaProperties properties)) {
            return bean;
        }

        String keyPrefix = binder
                .bind(VerificationProperties.PREFIX + ".key-prefix", String.class)
                .orElse("verification")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (isMissing(CAPTCHA_PREFIX, String.class)) {
            properties.setPrefix(keyPrefix + ":image:challenge");
        }
        if (properties.getExpire() == null) {
            properties.setExpire(new HashMap<>());
        }
        if (isMissing(DEFAULT_EXPIRE, Long.class)) {
            properties.getExpire().putIfAbsent("default", DEFAULT_CHALLENGE_EXPIRE_MILLIS);
        }
        if (isMissing(INIT_DEFAULT_RESOURCE, Boolean.class)) {
            properties.setInitDefaultResource(true);
        }

        SecondaryVerificationProperties secondary = properties.getSecondary();
        if (secondary == null) {
            secondary = new SecondaryVerificationProperties();
            properties.setSecondary(secondary);
        }
        if (isMissing(SECONDARY_ENABLED, Boolean.class)) {
            secondary.setEnabled(true);
        }
        if (isMissing(SECONDARY_EXPIRE, Long.class)) {
            secondary.setExpire(DEFAULT_SECONDARY_EXPIRE_MILLIS);
        }
        if (isMissing(SECONDARY_KEY_PREFIX, String.class)) {
            secondary.setKeyPrefix(keyPrefix + ":image:secondary");
        }
        return bean;
    }

    private <T> boolean isMissing(String name, Class<T> targetType) {
        return !binder.bind(name, targetType).isBound();
    }
}
