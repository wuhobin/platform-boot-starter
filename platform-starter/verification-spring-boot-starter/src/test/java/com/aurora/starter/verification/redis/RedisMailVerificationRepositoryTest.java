package com.aurora.starter.verification.redis;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.config.VerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisMailVerificationRepositoryTest {

    @Mock
    private RedisCache redisCache;

    private RedisMailVerificationRepository repository;

    @BeforeEach
    void setUp() {
        VerificationProperties properties = new VerificationProperties();
        properties.setKeyPrefix("APP:VERIFICATION");
        repository = new RedisMailVerificationRepository(redisCache, properties);
    }

    @Test
    void acquiresCooldownWithExpectedKeyAndTtl() {
        when(redisCache.setIfAbsent(any(), any(), any(Long.class), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        boolean acquired = repository.acquireCooldown(
                "user@example.com", "REGISTER", "token", Duration.ofSeconds(60));

        assertThat(acquired).isTrue();
        verify(redisCache).setIfAbsent(
                "app:verification:mail:cooldown:register:user@example.com",
                "token",
                60000L,
                TimeUnit.MILLISECONDS);
    }

    @Test
    void readsRemainingCooldown() {
        String key = "app:verification:mail:cooldown:login:user@example.com";
        when(redisCache.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(1234L);

        assertThat(repository.getCooldownRemaining("user@example.com", "LOGIN"))
                .isEqualTo(Duration.ofMillis(1234));
    }

    @Test
    @SuppressWarnings("unchecked")
    void storesPlainTextCodeOnlyWhileCooldownIsOwned() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(List.of(
                        "app:verification:mail:code:login:user@example.com",
                        "app:verification:mail:cooldown:login:user@example.com")),
                eq("owned-token"),
                eq("012345"),
                eq(300000L)))
                .thenReturn(1L);

        assertThat(repository.storeCodeIfCooldownOwned(
                "user@example.com", "LOGIN", "owned-token", "012345", Duration.ofMinutes(5)))
                .isTrue();

        verify(redisCache).execute(
                any(RedisScript.class),
                eq(List.of(
                        "app:verification:mail:code:login:user@example.com",
                        "app:verification:mail:cooldown:login:user@example.com")),
                eq("owned-token"),
                eq("012345"),
                eq(300000L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifiesAndConsumesWithLua() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(List.of("app:verification:mail:code:login:user@example.com")),
                eq("012345")))
                .thenReturn(1L);

        assertThat(repository.verifyAndConsume("user@example.com", "LOGIN", "012345"))
                .isTrue();

        verify(redisCache).execute(
                any(RedisScript.class),
                eq(List.of("app:verification:mail:code:login:user@example.com")),
                eq("012345"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void releasesOnlyOwnedCooldownWithLua() {
        repository.releaseCooldown("user@example.com", "LOGIN", "owned-token");

        verify(redisCache).execute(
                any(RedisScript.class),
                eq(List.of("app:verification:mail:cooldown:login:user@example.com")),
                eq("owned-token"));
    }
}
