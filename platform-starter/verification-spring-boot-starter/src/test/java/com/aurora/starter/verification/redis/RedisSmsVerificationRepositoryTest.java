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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSmsVerificationRepositoryTest {

    private static final Instant RESERVED_AT = Instant.parse("2026-07-31T08:00:00Z");

    @Mock
    private RedisCache redisCache;

    private RedisSmsVerificationRepository repository;

    @BeforeEach
    void setUp() {
        VerificationProperties properties = new VerificationProperties();
        properties.setKeyPrefix("APP:VERIFICATION");
        repository = new RedisSmsVerificationRepository(redisCache, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservesCodeCooldownAndCrossSceneQuotasAtomically() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                eq("token"),
                eq("012345"),
                eq(300000L),
                eq(60000L),
                eq(5),
                eq(3600000L),
                eq(10),
                eq(28800000L)))
                .thenReturn(1L);

        RedisSmsVerificationRepository.Reservation result = repository.reserve(
                "13800138000",
                "REGISTER",
                "token",
                "012345",
                RESERVED_AT,
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                5,
                10);

        assertThat(result.status())
                .isEqualTo(RedisSmsVerificationRepository.ReservationStatus.RESERVED);
        assertThat(result.retryAfter()).isZero();
        verify(redisCache).execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                eq("token"),
                eq("012345"),
                eq(300000L),
                eq(60000L),
                eq(5),
                eq(3600000L),
                eq(10),
                eq(28800000L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsCooldownRemainingTime() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(2L);
        when(redisCache.getExpire(
                "app:verification:sms:cooldown:register:13800138000",
                TimeUnit.MILLISECONDS))
                .thenReturn(1234L);

        RedisSmsVerificationRepository.Reservation result = reserve();

        assertThat(result.status())
                .isEqualTo(RedisSmsVerificationRepository.ReservationStatus.COOLDOWN);
        assertThat(result.retryAfter()).isEqualTo(Duration.ofMillis(1234));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsHourlyLimitAcrossScenes() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(3L);
        when(redisCache.getExpire(
                "app:verification:sms:quota:hourly:13800138000",
                TimeUnit.MILLISECONDS))
                .thenReturn(120000L);

        RedisSmsVerificationRepository.Reservation result = reserve();

        assertThat(result.status())
                .isEqualTo(RedisSmsVerificationRepository.ReservationStatus.HOURLY_LIMIT);
        assertThat(result.retryAfter()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rollsBackOnlyTheOwnedReservation() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                eq("token")))
                .thenReturn(1L);

        assertThat(repository.rollback(
                "13800138000", "REGISTER", "token", RESERVED_AT)).isTrue();

        verify(redisCache).execute(
                any(RedisScript.class),
                eq(reservationKeys()),
                eq("token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsFailedAttemptCountFromAtomicVerification() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(List.of(
                        "app:verification:sms:code:register:13800138000",
                        "app:verification:sms:attempts:register:13800138000")),
                eq("999999"),
                eq(5)))
                .thenReturn(-5L);

        RedisSmsVerificationRepository.VerificationResult result = repository.verifyAndConsume(
                "13800138000", "REGISTER", "999999", 5);

        assertThat(result.verified()).isFalse();
        assertThat(result.failedAttempts()).isEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSuccessAfterAtomicConsumption() {
        when(redisCache.execute(
                any(RedisScript.class),
                eq(List.of(
                        "app:verification:sms:code:register:13800138000",
                        "app:verification:sms:attempts:register:13800138000")),
                eq("012345"),
                eq(5)))
                .thenReturn(1L);

        RedisSmsVerificationRepository.VerificationResult result = repository.verifyAndConsume(
                "13800138000", "REGISTER", "012345", 5);

        assertThat(result.verified()).isTrue();
        assertThat(result.failedAttempts()).isZero();
    }

    private RedisSmsVerificationRepository.Reservation reserve() {
        return repository.reserve(
                "13800138000",
                "REGISTER",
                "token",
                "012345",
                RESERVED_AT,
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                5,
                10);
    }

    private List<String> reservationKeys() {
        return List.of(
                "app:verification:sms:code:register:13800138000",
                "app:verification:sms:attempts:register:13800138000",
                "app:verification:sms:cooldown:register:13800138000",
                "app:verification:sms:quota:hourly:13800138000",
                "app:verification:sms:quota:daily:20260731:13800138000");
    }
}
