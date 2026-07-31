package com.aurora.starter.webmvc.config;

import com.aurora.starter.webmvc.exception.GlobalExceptionHandler;
import com.aurora.starter.webmvc.exception.SaTokenExceptionHandler;
import com.aurora.starter.webmvc.filter.RequestLogFilter;
import com.aurora.starter.webmvc.filter.TraceIdFilter;
import com.aurora.starter.webmvc.handler.ResponseEncryptionAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformWebMvcAutoConfigurationTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformWebMvcAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void registersWebMvcInfrastructureWithoutComponentScanning() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context).hasSingleBean(SaTokenExceptionHandler.class);
            assertThat(context).hasSingleBean(TraceIdFilter.class);
            assertThat(context).hasSingleBean(RequestLogFilter.class);
            assertThat(context).hasSingleBean(ResponseEncryptionProperties.class);
            assertThat(context).doesNotHaveBean(ResponseEncryptionAdvice.class);
        });
    }

    @Test
    void registersResponseEncryptionWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.webmvc.response-encryption.enabled=true",
                        "platform.webmvc.response-encryption.key=" + VALID_KEY
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ResponseEncryptionAdvice.class);
                    assertThat(context).hasBean(
                            PlatformWebMvcAutoConfiguration.RESPONSE_ENCRYPTION_SECRET_KEY_BEAN_NAME);
                    SecretKey secretKey = context.getBean(
                            PlatformWebMvcAutoConfiguration.RESPONSE_ENCRYPTION_SECRET_KEY_BEAN_NAME,
                            SecretKey.class);
                    assertThat(secretKey.getEncoded()).hasSize(32);
                });
    }

    @Test
    void failsStartupWhenEnabledWithoutKey() {
        contextRunner
                .withPropertyValues("platform.webmvc.response-encryption.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("key must be configured");
                });
    }

    @Test
    void failsStartupWhenConfiguredKeyHasWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        contextRunner
                .withPropertyValues(
                        "platform.webmvc.response-encryption.enabled=true",
                        "platform.webmvc.response-encryption.key=" + shortKey
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("exactly 32 bytes");
                });
    }

    @Test
    void failsStartupWhenConfiguredKeyIsNotBase64() {
        contextRunner
                .withPropertyValues(
                        "platform.webmvc.response-encryption.enabled=true",
                        "platform.webmvc.response-encryption.key=not-base64!"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("must be valid Base64");
                });
    }

    @Test
    void backsOffWhenCustomResponseEncryptionAdviceExists() {
        contextRunner
                .withPropertyValues(
                        "platform.webmvc.response-encryption.enabled=true",
                        "platform.webmvc.response-encryption.key=" + VALID_KEY
                )
                .withUserConfiguration(CustomResponseEncryptionConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResponseEncryptionAdvice.class);
                    assertThat(context.getBean(ResponseEncryptionAdvice.class))
                            .isSameAs(context.getBean("customResponseEncryptionAdvice"));
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomResponseEncryptionConfiguration {

        @Bean
        ResponseEncryptionAdvice customResponseEncryptionAdvice() {
            return new ResponseEncryptionAdvice(
                    new ObjectMapper(),
                    new SecretKeySpec(new byte[32], "AES")
            );
        }
    }
}
