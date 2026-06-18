package com.aurora.starter.redis.config;

import com.aurora.starter.redis.core.JsonRedisTemplate;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.redis.core.RedisMessageQueue;
import com.aurora.starter.redis.core.RedisRateLimiter;
import org.redisson.Redisson;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis 自动配置.
 *
 * @author whb
 */
@AutoConfiguration
public class RedisAutoConfig {

    /**
     * JSON 序列化 RedisTemplate.
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        return new JsonRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedissonAutoConfigurationCustomizer jsonCustomizer() {
        return (c) -> c.setCodec(new JsonJacksonCodec());
    }

    @Bean
    public RedisCache redisCache(RedisTemplate redisTemplate){
        return new RedisCache(redisTemplate);
    }

    /**
     * 限流处理器.
     *
     * @param redissonClient redisson客户端
     * @return 限流处理器
     */
    @Bean
    public RedisRateLimiter redisRateLimiter(Redisson redissonClient) {
        return new RedisRateLimiter(redissonClient);
    }

    /**
     * Redis 消息阻塞队列.
     *
     * @param redissonClient redisson客户端
     * @return 消息阻塞队列
     */
    @Bean
    public RedisMessageQueue redisMessageQueue(Redisson redissonClient) {
        return new RedisMessageQueue(redissonClient);
    }
}
