package com.aurora.starter.redis.core.manager;

import com.aurora.starter.redis.core.TwoLevelCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 两级缓存实例管理器.
 * <p>
 * 统一管理所有 TwoLevelCache 实例（含默认实例和命名实例），
 * 业务通过 manager.get(name) 按名取用。
 *
 * @author whb
 */
@Slf4j
public class TwoLevelCacheManager implements InitializingBean {

    private final TwoLevelCache defaultCache;
    private final Map<String, TwoLevelCache> caches;

    public TwoLevelCacheManager(TwoLevelCache defaultCache, Map<String, TwoLevelCache> caches) {
        this.defaultCache = defaultCache;
        this.caches = Collections.unmodifiableMap(caches);
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Two-level cache manager initialized: {}", this);
    }

    /**
     * 获取指定名称的缓存实例.
     *
     * @param name 缓存实例名称
     * @return TwoLevelCache 实例
     * @throws IllegalArgumentException 如果 name 未在配置中声明
     */
    public TwoLevelCache get(String name) {
        TwoLevelCache cache = caches.get(name);
        if (cache == null) {
            throw new IllegalArgumentException(
                "Two-level cache [" + name + "] is not configured, available: " + caches.keySet());
        }
        return cache;
    }

    /**
     * 获取默认实例.
     */
    public TwoLevelCache getDefault() {
        return defaultCache;
    }

    /**
     * 返回所有已注册的实例名称（含 "default"）.
     */
    public Set<String> names() {
        return caches.keySet();
    }

    @Override
    public String toString() {
        return "TwoLevelCacheManager{" +
                "defaultCacheName='" + defaultCache.getName() + '\'' +
                ", totalInstances=" + caches.size() +
                ", names=" + caches.keySet() +
                '}';
    }
}
