package com.aurora.starter.redis.core;

import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TwoLevelCacheTemplateTest {

    private static final String CACHE_NAME = "featureFlags";
    private static final String CACHE_KEY = "feature-flag:dark-mode";

    private final TwoLevelCacheManager cacheManager = mock(TwoLevelCacheManager.class);
    private final TwoLevelCache twoLevelCache = mock(TwoLevelCache.class);
    private final TwoLevelCacheTemplate cacheTemplate = new TwoLevelCacheTemplate(cacheManager);

    @Test
    void supportsArbitraryCachedValueTypes() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.<Integer>get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(42);

        Integer value = cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, () -> 0, 30L, TimeUnit.SECONDS);

        assertThat(value).isEqualTo(42);
    }

    @Test
    void fallsBackToLoaderWhenRedisIsUnavailable() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        String value = cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, () -> "enabled", 30L, TimeUnit.SECONDS);

        assertThat(value).isEqualTo("enabled");
    }

    @Test
    void doesNotInvokeLoaderAgainWhenRedisFailsAfterTheLoader() {
        AtomicInteger loaderCalls = new AtomicInteger();
        Supplier<String> loader = () -> {
            loaderCalls.incrementAndGet();
            return "enabled";
        };
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<String> guardedLoader = invocation.getArgument(1);
                    guardedLoader.get();
                    throw new RedisConnectionFailureException("redis write unavailable");
                });

        assertThat(cacheTemplate.get(CACHE_NAME, CACHE_KEY, loader, 30L, TimeUnit.SECONDS))
                .isEqualTo("enabled");
        assertThat(loaderCalls).hasValue(1);
    }

    @Test
    void propagatesLoaderRedisFailuresInsteadOfTreatingThemAsCacheFailures() {
        AtomicInteger loaderCalls = new AtomicInteger();
        Supplier<String> loader = () -> {
            loaderCalls.incrementAndGet();
            throw new RedisConnectionFailureException("data source unavailable");
        };
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<String> guardedLoader = invocation.getArgument(1);
                    return guardedLoader.get();
                });

        assertThatThrownBy(() -> cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, loader, 30L, TimeUnit.SECONDS))
                .isInstanceOf(RedisConnectionFailureException.class)
                .hasMessage("data source unavailable");
        assertThat(loaderCalls).hasValue(1);
    }

    @Test
    void propagatesFailuresThatAreNotCausedByRedis() {
        AtomicInteger loaderCalls = new AtomicInteger();
        Supplier<String> loader = () -> {
            loaderCalls.incrementAndGet();
            return "enabled";
        };
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        when(twoLevelCache.get(eq(CACHE_KEY), any(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenThrow(new IllegalStateException("invalid cache configuration"));

        assertThatThrownBy(() -> cacheTemplate.get(
                CACHE_NAME, CACHE_KEY, loader, 30L, TimeUnit.SECONDS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid cache configuration");
        assertThat(loaderCalls).hasValue(0);
    }

    @Test
    void writesAndEvictsImmediatelyWithoutAnActiveTransaction() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);

        cacheTemplate.replaceAfterCommitBestEffort(
                CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        cacheTemplate.evictAfterCommitBestEffort(CACHE_NAME, CACHE_KEY);

        var ordered = inOrder(twoLevelCache);
        ordered.verify(twoLevelCache).evict(CACHE_KEY);
        ordered.verify(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        ordered.verify(twoLevelCache).evict(CACHE_KEY);
    }

    @Test
    void defersWritesAndEvictionsUntilTheActiveTransactionCommits() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            cacheTemplate.replaceAfterCommitBestEffort(
                    CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            cacheTemplate.evictAfterCommitBestEffort(CACHE_NAME, CACHE_KEY);

            verifyNoInteractions(cacheManager);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            var ordered = inOrder(twoLevelCache);
            ordered.verify(twoLevelCache).evict(CACHE_KEY);
            ordered.verify(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            ordered.verify(twoLevelCache).evict(CACHE_KEY);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void ignoresCacheWriteAndEvictionFailures() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(twoLevelCache).evict(CACHE_KEY);

        assertThatCode(() -> {
            cacheTemplate.setBestEffort(
                    CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);
            cacheTemplate.evictBestEffort(CACHE_NAME, CACHE_KEY);
        }).doesNotThrowAnyException();
    }

    @Test
    void requiredWritesAndEvictionsPropagateFailures() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        doThrow(new RedisConnectionFailureException("redis set unavailable"))
                .when(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);

        assertThatThrownBy(() -> cacheTemplate.setRequired(
                CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS))
                .isInstanceOf(RedisConnectionFailureException.class)
                .hasMessageContaining("redis set unavailable");

        doThrow(new RedisConnectionFailureException("redis evict unavailable"))
                .when(twoLevelCache).evict(CACHE_KEY);

        assertThatThrownBy(() -> cacheTemplate.evictRequired(CACHE_NAME, CACHE_KEY))
                .isInstanceOf(RedisConnectionFailureException.class)
                .hasMessageContaining("redis evict unavailable");
    }

    @Test
    void compensatesAFailedPostCommitWriteWithAnotherEviction() {
        when(cacheManager.get(CACHE_NAME)).thenReturn(twoLevelCache);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(twoLevelCache).set(CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS);

        assertThatCode(() -> cacheTemplate.replaceAfterCommitBestEffort(
                CACHE_NAME, CACHE_KEY, "enabled", 30L, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

        verify(twoLevelCache, org.mockito.Mockito.times(2)).evict(CACHE_KEY);
    }
}
