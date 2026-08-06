package com.aurora.starter.webmvc.config;

import com.aurora.starter.webmvc.exception.GlobalExceptionHandler;
import com.aurora.starter.webmvc.exception.SaTokenExceptionHandler;
import com.aurora.starter.webmvc.filter.RequestLogFilter;
import com.aurora.starter.webmvc.filter.TraceIdFilter;
import com.aurora.starter.webmvc.handler.ResponseEncryptionAdvice;
import com.aurora.starter.webmvc.security.PlatformCredentialCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(PlatformWebMvcProperties.class)
public class PlatformWebMvcAutoConfiguration {

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

    @Bean
    @ConditionalOnMissingBean
    public PlatformCredentialCipher platformCredentialCipher(PlatformWebMvcProperties properties) {
        return new PlatformCredentialCipher(properties.getCredentialSecretKey());
    }

    @Bean
    @ConditionalOnMissingBean(ResponseEncryptionAdvice.class)
    public ResponseEncryptionAdvice responseEncryptionAdvice(
            ObjectMapper objectMapper,
            PlatformWebMvcProperties properties) {
        return new ResponseEncryptionAdvice(objectMapper, properties.getResponseSecretKey());
    }
}
