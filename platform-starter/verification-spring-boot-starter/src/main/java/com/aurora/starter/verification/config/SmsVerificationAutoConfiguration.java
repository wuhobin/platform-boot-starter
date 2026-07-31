package com.aurora.starter.verification.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.redis.RedisSmsVerificationRepository;
import com.aurora.starter.verification.sms.DefaultSmsVerificationService;
import com.aurora.starter.verification.sms.SmsVerificationService;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 阿里云短信验证码自动配置。
 */
@AutoConfiguration(after = VerificationAutoConfiguration.class, afterName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "com.aurora.starter.redis.config.RedisAutoConfig"
})
@ConditionalOnClass(Client.class)
@ConditionalOnProperty(
        prefix = "platform.verification.sms",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class SmsVerificationAutoConfiguration {

    static final String CLIENT_BEAN_NAME = "aliyunSmsVerificationClient";
    private static final String ENDPOINT = "dypnsapi.aliyuncs.com";

    @Bean(CLIENT_BEAN_NAME)
    @ConditionalOnMissingBean(name = CLIENT_BEAN_NAME)
    public Client aliyunSmsVerificationClient(VerificationProperties properties) throws Exception {
        VerificationProperties.SmsProperties sms = properties.getSms();
        Config config = new Config()
                .setAccessKeyId(sms.getAccessKeyId().trim())
                .setAccessKeySecret(sms.getAccessKeySecret().trim())
                .setEndpoint(ENDPOINT);
        return new Client(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationCodeGenerator smsVerificationCodeGenerator() {
        return new VerificationCodeGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisSmsVerificationRepository redisSmsVerificationRepository(
            RedisCache redisCache,
            VerificationProperties properties) {
        return new RedisSmsVerificationRepository(redisCache, properties);
    }

    @Bean
    @ConditionalOnMissingBean(SmsVerificationService.class)
    public SmsVerificationService smsVerificationService(
            @Qualifier(CLIENT_BEAN_NAME) Client client,
            RedisSmsVerificationRepository repository,
            VerificationCodeGenerator codeGenerator,
            VerificationProperties properties) {
        return new DefaultSmsVerificationService(client, repository, codeGenerator, properties);
    }
}
