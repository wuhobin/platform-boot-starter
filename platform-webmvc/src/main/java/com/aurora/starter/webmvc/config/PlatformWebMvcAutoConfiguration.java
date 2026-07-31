package com.aurora.starter.webmvc.config;

import com.aurora.starter.webmvc.exception.GlobalExceptionHandler;
import com.aurora.starter.webmvc.exception.SaTokenExceptionHandler;
import com.aurora.starter.webmvc.filter.RequestLogFilter;
import com.aurora.starter.webmvc.filter.TraceIdFilter;
import com.aurora.starter.webmvc.handler.ResponseEncryptionAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ResponseEncryptionProperties.class)
public class PlatformWebMvcAutoConfiguration {

    static final String RESPONSE_ENCRYPTION_SECRET_KEY_BEAN_NAME = "responseEncryptionSecretKey";

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenExceptionHandler saTokenExceptionHandler() {
        return new SaTokenExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    @Bean(RESPONSE_ENCRYPTION_SECRET_KEY_BEAN_NAME)
    @ConditionalOnProperty(
            prefix = ResponseEncryptionProperties.PREFIX,
            name = "enabled",
            havingValue = "true"
    )
    public SecretKey responseEncryptionSecretKey(ResponseEncryptionProperties properties) {
        String configuredKey = properties.getKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    ResponseEncryptionProperties.PREFIX + ".key must be configured when response encryption is enabled");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    ResponseEncryptionProperties.PREFIX + ".key must be valid Base64", exception);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    ResponseEncryptionProperties.PREFIX + ".key must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Bean
    @ConditionalOnMissingBean(ResponseEncryptionAdvice.class)
    @ConditionalOnProperty(
            prefix = ResponseEncryptionProperties.PREFIX,
            name = "enabled",
            havingValue = "true"
    )
    public ResponseEncryptionAdvice responseEncryptionAdvice(
            ObjectMapper objectMapper,
            @Qualifier(RESPONSE_ENCRYPTION_SECRET_KEY_BEAN_NAME) SecretKey secretKey) {
        return new ResponseEncryptionAdvice(objectMapper, secretKey);
    }
}
