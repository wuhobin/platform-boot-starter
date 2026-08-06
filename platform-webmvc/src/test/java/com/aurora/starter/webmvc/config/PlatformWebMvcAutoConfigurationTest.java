package com.aurora.starter.webmvc.config;

import com.aurora.starter.webmvc.exception.GlobalExceptionHandler;
import com.aurora.starter.webmvc.exception.SaTokenExceptionHandler;
import com.aurora.starter.webmvc.filter.RequestLogFilter;
import com.aurora.starter.webmvc.filter.TraceIdFilter;
import com.aurora.starter.webmvc.handler.ResponseEncryptionAdvice;
import com.aurora.starter.webmvc.security.PlatformCredentialCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(context).hasSingleBean(PlatformWebMvcProperties.class);
            assertThat(context).hasSingleBean(PlatformCredentialCipher.class);
            assertThat(context).hasSingleBean(ResponseEncryptionAdvice.class);
        });
    }

    @Test
    void bindsBothEncryptionSecretsWithoutValidatingThemAtStartup() {
        contextRunner
                .withPropertyValues(
                        "platform.webmvc.response-secret-key=" + VALID_KEY,
                        "platform.webmvc.credential-secret-key=" + VALID_KEY
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ResponseEncryptionAdvice.class);
                    assertThat(context.getBean(PlatformWebMvcProperties.class).getResponseSecretKey())
                            .isEqualTo(VALID_KEY);
                    assertThat(context.getBean(PlatformWebMvcProperties.class).getCredentialSecretKey())
                            .isEqualTo(VALID_KEY);
                });
    }

    @Test
    void keepsContextAvailableWhenSecretsAreMissing() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(PlatformCredentialCipher.class)
                            .encrypt("test-purpose", "secret"))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("credential-secret-key");
                });
    }

    @Test
    void backsOffWhenCustomResponseEncryptionAdviceExists() {
        contextRunner
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
                    new ObjectMapper(), VALID_KEY
            );
        }
    }
}
