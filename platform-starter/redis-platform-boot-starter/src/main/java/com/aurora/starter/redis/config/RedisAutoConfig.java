package com.aurora.starter.redis.config;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.JsonRedisTemplate;
import com.aurora.starter.redis.core.RedisBloomFilter;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.redis.core.RedisMessageQueue;
import com.aurora.starter.redis.core.RedisRateLimiter;
import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.TwoLevelCacheManager;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 自动配置.
 *
 * @author whb
 */
@AutoConfiguration
@EnableConfigurationProperties({BloomFilterProperties.class, TwoLevelCacheProperties.class})
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
    public RedisRateLimiter redisRateLimiter(RedissonClient redissonClient) {
        return new RedisRateLimiter(redissonClient);
    }

    /**
     * Redis 消息阻塞队列.
     *
     * @param redissonClient redisson客户端
     * @return 消息阻塞队列
     */
    @Bean
    public RedisMessageQueue redisMessageQueue(RedissonClient redissonClient) {
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
            RedissonClient redissonClient,
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

    /**
     * 两级缓存管理器，统一管理所有 TwoLevelCache 实例.
     * <p>
     * 仅当 platform.redis.two-level-cache.enabled=true 时生效。
     * 必须声明在 redisCache() Bean 之后，确保依赖就绪。
     *
     * @param redisCache      Redis 缓存操作
     * @param redissonClient redisson 客户端
     * @param properties     两级缓存配置
     * @return TwoLevelCacheManager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.redis.two-level-cache", name = "enabled", havingValue = "true")
    public TwoLevelCacheManager twoLevelCacheManager(
            RedisCache redisCache,
            RedissonClient redissonClient,
            TwoLevelCacheProperties properties) {

        // 默认实例（先创建，后续配置中可能有 name="default" 的覆盖定义）
        TwoLevelCache defaultCache = null;

        // 命名实例（含重名检测）
        Map<String, TwoLevelCache> instances = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (TwoLevelCacheProperties.InstanceConfig cfg : properties.getInstances()) {
            if (!seen.add(cfg.getName())) {
                throw new IllegalStateException(
                    "Duplicate two-level cache name [" + cfg.getName() + "] in configuration");
            }
            long maxSize = cfg.getMaxSize() != null ? cfg.getMaxSize() : properties.getMaxSize();
            long ttl = cfg.getDefaultTtl() != null
                ? cfg.getDefaultTtl().getSeconds() : properties.getDefaultTtl().getSeconds();
            TwoLevelCache instance = new TwoLevelCache(cfg.getName(), redisCache, redissonClient, maxSize, ttl);
            if ("default".equals(cfg.getName())) {
                defaultCache = instance;
            } else {
                instances.put(cfg.getName(), instance);
            }
        }

        if (defaultCache == null) {
            defaultCache = new TwoLevelCache("default", redisCache, redissonClient,
                properties.getMaxSize(), properties.getDefaultTtl().getSeconds());
        }
        instances.put("default", defaultCache);
        return new TwoLevelCacheManager(defaultCache, instances);
    }
}
