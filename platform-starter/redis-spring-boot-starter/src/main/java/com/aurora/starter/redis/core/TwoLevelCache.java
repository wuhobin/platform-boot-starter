package com.aurora.starter.redis.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 两级缓存实例.
 * <p>
 * L1: Caffeine 本地缓存，L2: Redis 分布式缓存。
 * 每个实例通过 name 隔离，拥有独立的 Caffeine 和 Pub/Sub Topic。
 *
 * @author whb
 */
@Slf4j
public class TwoLevelCache implements InitializingBean {

    static final String TOPIC_PREFIX = "two-level-cache:evict:";
    static final long NULL_VALUE_TTL_SECONDS = 60;
    static final Object NULL_VALUE = new Object();

    @Getter
    private final String name;

    private final Cache<String, Object> cache;
    private final ConcurrentMap<String, Long> ttlMap = new ConcurrentHashMap<>();
    private final RedisCache redisCache;
    private final RTopic topic;
    private final long defaultTtlSeconds;

    public TwoLevelCache(String name, RedisCache redisCache,
                         RedissonClient redissonClient,
                         long maxSize, long defaultTtlSeconds) {
        this.name = name;
        this.redisCache = redisCache;
        this.defaultTtlSeconds = defaultTtlSeconds;

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        return ttlMap.getOrDefault(key, TimeUnit.SECONDS.toNanos(defaultTtlSeconds));
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value,
                                                  long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, Object value,
                                                long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .removalListener((String key, Object value, RemovalCause cause) -> {
                    ttlMap.remove(key);
                })
                .build();

        this.topic = redissonClient.getTopic(TOPIC_PREFIX + name);
    }

    @Override
    public void afterPropertiesSet() {
        topic.addListener(String.class, (channel, msg) -> {
            for (String key : msg.split(",")) {
                cache.invalidate(key);
            }
        });
    }

    // === 读 ===

    /**
     * 纯读，L1 → L2 穿透，不回源.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object val = cache.getIfPresent(key);
        if (val != null) {
            return val == NULL_VALUE ? null : (T) val;
        }
        val = redisCache.getCacheObject(key);
        if (val != null) {
            cache.put(key, val);
            return (T) val;
        }
        return null;
    }

    /**
     * 带回源 + 默认 TTL.
     */
    public <T> T get(String key, Supplier<T> loader) {
        return get(key, loader, defaultTtlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 带回源 + 自定义 TTL.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit unit) {
        // 1. L1
        Object val = cache.getIfPresent(key);
        if (val != null) {
            return val == NULL_VALUE ? null : (T) val;
        }

        // 2. L2
        val = redisCache.getCacheObject(key);
        if (val != null) {
            putL1(key, val, ttl, unit);
            return (T) val;
        }

        // 3. 回源 — Caffeine get 合并同 key 并发
        try {
            Object result = cache.get(key, k -> {
                Object loaded = loader.get();
                if (loaded != null) {
                    ttlMap.put(k, unit.toNanos(ttl));
                    redisCache.setCacheObject(k, loaded, ttl, unit);
                    return loaded;
                }
                ttlMap.put(k, TimeUnit.SECONDS.toNanos(NULL_VALUE_TTL_SECONDS));
                return NULL_VALUE;
            });
            return result == NULL_VALUE ? null : (T) result;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        }
    }

    // === 写 ===

    /**
     * 写穿 L1 + L2 + Pub/Sub 广播.
     * <p>
     * 注意：本实例也会收到自己广播的失效消息，导致刚写入的 L1 被删除。
     * 影响很小——下次 get() 会从 L2 命中回填 L1，仅多一次 L1 miss。
     */
    public void set(String key, Object value, long ttl, TimeUnit unit) {
        putL1(key, value, ttl, unit);
        redisCache.setCacheObject(key, value, ttl, unit);
        topic.publish(key);
    }

    // === 删 ===

    /**
     * 删 L1 + L2 + Pub/Sub 广播.
     */
    public void evict(String key) {
        cache.invalidate(key);
        ttlMap.remove(key);
        redisCache.deleteObject(key);
        topic.publish(key);
    }

    /**
     * 批量删除.
     */
    public void evict(Collection<String> keys) {
        cache.invalidateAll(keys);
        keys.forEach(ttlMap::remove);
        redisCache.deleteObject(keys);
        // 发送单条包含所有 key 的通知，避免 N 次 PUBLISH
        topic.publish(String.join(",", keys));
    }

    // === 本地 ===

    /**
     * 仅清 L1，不操作 Redis.
     */
    public void clearLocal() {
        cache.invalidateAll();
        ttlMap.clear();  // 同步清空，防止异步 RemovalListener 尚未执行
    }

    private void putL1(String key, Object value, long ttl, TimeUnit unit) {
        ttlMap.put(key, unit.toNanos(ttl));
        cache.put(key, value);
        log.debug("L1 cache [{}] put key [{}] with ttl {}s", name, key, unit.toSeconds(ttl));
    }
}
