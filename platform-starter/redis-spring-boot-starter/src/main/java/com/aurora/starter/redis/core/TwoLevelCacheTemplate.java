package com.aurora.starter.redis.core;

import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 通用二级缓存操作模板。
 * <p>
 * 读取缓存时 Redis 不可用会回退到数据加载器。修改方法通过 Required 与
 * BestEffort 后缀明确区分异常是否向上传递，并支持在当前事务提交后执行。
 */
@Slf4j
public class TwoLevelCacheTemplate {

    private final TwoLevelCacheManager cacheManager;

    public TwoLevelCacheTemplate(TwoLevelCacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager must not be null");
    }

    /**
     * 读取二级缓存，Redis 基础设施故障时回源。
     * <p>
     * 回源值会被记住，因此缓存回填阶段发生 Redis 故障时不会再次执行 loader。
     */
    public <T> T get(String cacheName, String cacheKey, Supplier<T> loader,
                     long ttl, TimeUnit timeUnit) {
        Objects.requireNonNull(loader, "loader must not be null");
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        AtomicBoolean loaderCalled = new AtomicBoolean();
        AtomicReference<T> loadedValue = new AtomicReference<>();
        Supplier<T> guardedLoader = () -> {
            loaderCalled.set(true);
            try {
                T value = loader.get();
                loadedValue.set(value);
                return value;
            } catch (RuntimeException exception) {
                // Do not mistake a data-source Redis exception for a cache infrastructure failure.
                throw new CacheLoaderException(exception);
            }
        };

        try {
            return cache(cacheName).get(cacheKey, guardedLoader, ttl, timeUnit);
        } catch (CacheLoaderException exception) {
            throw exception.originalException();
        } catch (RuntimeException exception) {
            if (!isRedisFailure(exception)) {
                throw exception;
            }
            log.warn("Redis unavailable while reading two-level cache [{}] key [{}], falling back to loader",
                    cacheName, cacheKey, exception);
            return loaderCalled.get() ? loadedValue.get() : loader.get();
        }
    }

    public void setBestEffort(String cacheName, String cacheKey, Object value,
                              long ttl, TimeUnit timeUnit) {
        try {
            cache(cacheName).set(cacheKey, value, ttl, timeUnit);
        } catch (RuntimeException exception) {
            log.warn("Failed to write two-level cache [{}] key [{}]", cacheName, cacheKey, exception);
        }
    }

    public void setRequired(String cacheName, String cacheKey, Object value,
                            long ttl, TimeUnit timeUnit) {
        cache(cacheName).set(cacheKey, value, ttl, timeUnit);
    }

    public void evictBestEffort(String cacheName, String cacheKey) {
        try {
            cache(cacheName).evict(cacheKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to evict two-level cache [{}] key [{}]", cacheName, cacheKey, exception);
        }
    }

    public void evictRequired(String cacheName, String cacheKey) {
        cache(cacheName).evict(cacheKey);
    }

    public void evictAfterCommitBestEffort(String cacheName, String cacheKey) {
        runAfterCommit(() -> evictBestEffort(cacheName, cacheKey));
    }

    public void replaceAfterCommitBestEffort(String cacheName, String cacheKey, Object value,
                                             long ttl, TimeUnit timeUnit) {
        runAfterCommit(() -> replaceBestEffort(cacheName, cacheKey, value, ttl, timeUnit));
    }

    private void replaceBestEffort(String cacheName, String cacheKey, Object value,
                                   long ttl, TimeUnit timeUnit) {
        try {
            evictRequired(cacheName, cacheKey);
            setRequired(cacheName, cacheKey, value, ttl, timeUnit);
        } catch (RuntimeException exception) {
            log.error("Failed to replace two-level cache [{}] key [{}] after database commit",
                    cacheName, cacheKey, exception);
            evictBestEffort(cacheName, cacheKey);
        }
    }

    private TwoLevelCache cache(String cacheName) {
        return cacheManager.get(cacheName);
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private static boolean isRedisFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.startsWith("org.springframework.data.redis.")
                    || className.startsWith("org.redisson.")
                    || className.startsWith("io.lettuce.core.")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class CacheLoaderException extends RuntimeException {

        private CacheLoaderException(RuntimeException originalException) {
            super(originalException);
        }

        private RuntimeException originalException() {
            return (RuntimeException) getCause();
        }
    }
}
