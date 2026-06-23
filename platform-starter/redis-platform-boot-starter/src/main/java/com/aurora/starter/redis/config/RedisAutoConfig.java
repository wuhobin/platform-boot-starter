package com.aurora.starter.redis.config;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.JsonRedisTemplate;
import com.aurora.starter.redis.core.RedisBloomFilter;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.redis.core.RedisMessageQueue;
import com.aurora.starter.redis.core.RedisRateLimiter;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis 自动配置.
 *
 * @author whb
 */
@AutoConfiguration
@EnableConfigurationProperties(BloomFilterProperties.class)
public class RedisAutoConfig {

    private static final String BLOOM_FILTER_KEY_PREFIX = "BLOOM_FILTER";

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

    /**
     * 布隆过滤器 Map，key 为配置中的 name，value 为对应的 RedisBloomFilter 实例.
     * <p>
     * 仅当 platform.redis.bloom-filter.enabled=true 时生效。
     *
     * @param redissonClient redisson 客户端
     * @param properties     布隆过滤器配置属性
     * @return name -> RedisBloomFilter 的 Map
     */
    @Bean(name = "redisBloomFilters")
    @ConditionalOnMissingBean(name = "redisBloomFilters")
    @ConditionalOnProperty(prefix = "platform.redis.bloom-filter", name = "enabled", havingValue = "true")
    public Map<String, RedisBloomFilter<?>> redisBloomFilters(
            Redisson redissonClient,
            BloomFilterProperties properties) {

        return properties.getFilters().stream().collect(Collectors.toMap(
            BloomFilterProperties.FilterConfig::getName,
            cfg -> {
                String cacheKey = RedisKeyUtil.generate(BLOOM_FILTER_KEY_PREFIX, cfg.getName());
                RBloomFilter<Object> rf = redissonClient.getBloomFilter(cacheKey);
                // tryInit returns false if already exists (e.g., restart) — that's OK, just use it
                rf.tryInit(cfg.getExpectedInsertions(), cfg.getFalsePositiveProbability());
                if (cfg.getTtl() != null) {
                    rf.expire(cfg.getTtl());
                }
                return new RedisBloomFilter<>(rf, cfg.getName(),
                    cfg.getExpectedInsertions(), cfg.getFalsePositiveProbability());
            }
        ));
    }
}
