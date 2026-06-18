package com.aurora.starter.redis.core;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.redis.exception.RateLimiterException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis限流处理器.
 *
 * @author whb
 * @date 2026-06-18 13:49
 */
@Slf4j
@AllArgsConstructor
public class RedisRateLimiter {

    private static final String REDIS_RATE_LIMITER_PREFIX_KEY = "RATE_LIMITER";

    private static final long DEFAULT_EXPIRE_DAYS = 1;

    private final ConcurrentMap<String, Boolean> configuredKeys = new ConcurrentHashMap<>();

    private final Redisson redissonClient;

    /**
     * 限流.
     * 会一直等待获取许可进行处理为止
     *
     * @param limitKey     限流key
     * @param rate         限流次数
     * @param rateInterval 限流时间(单位：秒)
     * @param runnable     业务处理
     */
    public void rateLimit(final String limitKey, final long rate,
                          final long rateInterval, final Runnable runnable) {
        rateLimit(limitKey, rate, rateInterval, RateIntervalUnit.SECONDS, runnable);
    }

    /**
     * 限流.
     * 会一直等待获取许可进行处理为止
     *
     * @param limitKey         限流key
     * @param rate             限流次数
     * @param rateInterval     限流时间
     * @param rateIntervalUnit 限流时间单位
     * @param runnable         业务处理
     */
    public void rateLimit(final String limitKey, final long rate, final long rateInterval,
                          final RateIntervalUnit rateIntervalUnit, final Runnable runnable) {
        // 获取限流
        RRateLimiter rateLimiter = getRateLimiter(limitKey, rate, rateInterval, rateIntervalUnit);
        // 开启限流
        rateLimiter.acquire();
        // 执行业务
        runnable.run();
    }

    /**
     * 尝试限流.
     * 不等待超时后直接丢弃任务不处理
     *
     * @param limitKey     限流key
     * @param rate         限流次数
     * @param rateInterval 限流时间(单位：秒)
     * @param runnable     业务处理
     */
    public boolean tryRateLimit(final String limitKey, final long rate, final long rateInterval, final Runnable runnable) {
        return tryRateLimit(limitKey, rate, rateInterval, RateIntervalUnit.SECONDS, 0, runnable);
    }

    /**
     * 尝试限流.
     * 在等待时间超时后直接丢弃任务不处理
     *
     * @param limitKey       限流key
     * @param rate           限流次数
     * @param rateInterval   限流时间(单位：秒)
     * @param waitTimeSeconds 等待时间（秒）
     * @param runnable       业务处理
     */
    public boolean tryRateLimit(final String limitKey, final long rate, final long rateInterval,
                             final long waitTimeSeconds, final Runnable runnable) {
        return tryRateLimit(limitKey, rate, rateInterval, RateIntervalUnit.SECONDS, waitTimeSeconds, runnable);
    }

    /**
     * 尝试限流.
     * 在等待时间超时后直接丢弃任务不处理
     *
     * @param limitKey         限流key
     * @param rate             限流次数
     * @param rateInterval     限流时间
     * @param rateIntervalUnit 限流时间单位
     * @param waitTimeSeconds  等待时间（秒）
     * @param runnable         业务处理
     * @return true：成功获取许可、false：限流中
     */
    public boolean tryRateLimit(final String limitKey, final long rate, final long rateInterval,
                             final RateIntervalUnit rateIntervalUnit, final long waitTimeSeconds,
                             final Runnable runnable) {
        // 获取限流
        RRateLimiter rateLimiter = getRateLimiter(limitKey, rate, rateInterval, rateIntervalUnit);
        // 开启限流
        if (!rateLimiter.tryAcquire(waitTimeSeconds, TimeUnit.SECONDS)) {
            log.warn("获取【{}】限流处理器超时,丢弃任务不处理", limitKey);
            return false;
        }
        // 执行业务
        runnable.run();
        return true;
    }

    /**
     * 限流处理.
     * 会一直等待获取许可进行处理
     *
     * @param limitKey     限流key
     * @param rate         限流次数
     * @param rateInterval 限流时间(单位：秒)
     * @param supplier     业务处理
     * @param <T>          限流返回值
     * @return 限流结果
     */
    public <T> T rateLimit(final String limitKey, final long rate,
                           final long rateInterval, final Supplier<T> supplier) {
        return rateLimit(limitKey, rate, rateInterval, RateIntervalUnit.SECONDS, supplier);
    }

    /**
     * 限流.
     * 会一直等待获取许可进行处理
     *
     * @param limitKey         限流key
     * @param rate             限流次数
     * @param rateInterval     限流时间
     * @param rateIntervalUnit 限流时间单位
     * @param supplier         业务处理
     * @param <T>              限流返回值
     * @return 限流结果
     */
    public <T> T rateLimit(final String limitKey, final long rate, final long rateInterval,
                           final RateIntervalUnit rateIntervalUnit, final Supplier<T> supplier) {
        // 获取限流
        RRateLimiter rateLimiter = getRateLimiter(limitKey, rate, rateInterval, rateIntervalUnit);
        // 开启限流
        rateLimiter.acquire();
        // 执行业务
        return supplier.get();
    }

    /**
     * 尝试限流.
     * 在等待时间超时后直接丢弃任务不处理
     *
     * @param limitKey       限流key
     * @param rate           限流次数
     * @param rateInterval   限流时间(单位：秒)
     * @param waitTimeSeconds 等待时间（秒）
     * @param supplier       业务处理
     */
    public <T> T tryRateLimit(final String limitKey, final long rate, final long rateInterval,
                             final long waitTimeSeconds, final Supplier<T> supplier) {
        return tryRateLimit(limitKey, rate, rateInterval, RateIntervalUnit.SECONDS, waitTimeSeconds, supplier);
    }

    /**
     * 尝试限流.
     * 在等待时间超时后直接丢弃任务不处理
     *
     * @param limitKey         限流key
     * @param rate             限流次数
     * @param rateInterval     限流时间
     * @param rateIntervalUnit 限流时间单位
     * @param waitTimeSeconds  等待时间（秒）
     * @param supplier         业务处理
     */
    public <T> T tryRateLimit(final String limitKey, final long rate, final long rateInterval,
                             final RateIntervalUnit rateIntervalUnit, final long waitTimeSeconds,
                              final Supplier<T> supplier) {
        // 获取限流
        RRateLimiter rateLimiter = getRateLimiter(limitKey, rate, rateInterval, rateIntervalUnit);
        // 开启限流
        if (!rateLimiter.tryAcquire(waitTimeSeconds, TimeUnit.SECONDS)) {
            log.warn("获取【{}】限流处理器超时,抛出异常", limitKey);
            throw new RateLimiterException();
        }
        // 执行业务
        return supplier.get();
    }

    /**
     * 尝试非阻塞获取限流许可，不执行任何业务逻辑.
     *
     * @param limitKey 限流Key
     * @return true：成功获取许可、false：限流中
     */
    public boolean tryAcquire(final String limitKey) {
        String cacheKey = getCacheKey(limitKey);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(cacheKey);
        return rateLimiter.tryAcquire();
    }

    /**
     * 获取限流器.
     *
     * @param limitKey     限流key
     * @param rate         限流次数
     * @param rateInterval 限流时间
     * @param rateIntervalUnit 限流时间单位
     * @return 限流器
     */
    private RRateLimiter getRateLimiter(final String limitKey, final long rate, final long rateInterval, final RateIntervalUnit rateIntervalUnit) {
        String cacheKey = getCacheKey(limitKey);
        // 获取限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(cacheKey);
        // 仅在首次配置时设置 rate 和过期时间
        configuredKeys.computeIfAbsent(cacheKey, k -> {
            rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, rateIntervalUnit);
            rateLimiter.expire(Duration.ofDays(DEFAULT_EXPIRE_DAYS));
            return Boolean.TRUE;
        });
        return rateLimiter;
    }

    /**
     * 获取限流缓存Key.
     *
     * @param limitKey 限流Key
     * @return 限流缓存Key
     */
    private String getCacheKey(final String limitKey) {
        if (StringUtils.isBlank(limitKey)) {
            throw new IllegalArgumentException("限流Key不能为空");
        }
        return RedisKeyUtil.generate(REDIS_RATE_LIMITER_PREFIX_KEY, limitKey);
    }

}
