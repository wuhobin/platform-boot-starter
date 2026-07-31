package com.aurora.starter.verification.redis;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.config.VerificationProperties;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码 Redis 存储、发送预占和配额控制。
 */
public class RedisSmsVerificationRepository {

    private static final String SMS = "sms";
    private static final String CODE = "code";
    private static final String ATTEMPTS = "attempts";
    private static final String COOLDOWN = "cooldown";
    private static final String QUOTA = "quota";
    private static final String HOURLY = "hourly";
    private static final String DAILY = "daily";
    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration HOURLY_WINDOW = Duration.ofHours(1);

    private static final long RESERVED = 1L;
    private static final long COOLDOWN_REJECTED = 2L;
    private static final long HOURLY_REJECTED = 3L;
    private static final long DAILY_REJECTED = 4L;

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[3]) == 1 then return 2 end; "
                    + "local hourly = tonumber(redis.call('get', KEYS[4]) or '0'); "
                    + "if hourly >= tonumber(ARGV[5]) then return 3 end; "
                    + "local daily = tonumber(redis.call('get', KEYS[5]) or '0'); "
                    + "if daily >= tonumber(ARGV[7]) then return 4 end; "
                    + "redis.call('psetex', KEYS[3], ARGV[4], ARGV[1]); "
                    + "local newHourly = redis.call('incr', KEYS[4]); "
                    + "if newHourly == 1 then redis.call('pexpire', KEYS[4], ARGV[6]) end; "
                    + "local newDaily = redis.call('incr', KEYS[5]); "
                    + "if newDaily == 1 then redis.call('pexpire', KEYS[5], ARGV[8]) end; "
                    + "redis.call('psetex', KEYS[1], ARGV[3], ARGV[1] .. ':' .. ARGV[2]); "
                    + "redis.call('del', KEYS[2]); return 1",
            Long.class);

    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('get', KEYS[1]); "
                    + "local prefix = ARGV[1] .. ':'; "
                    + "if not value or string.sub(value, 1, string.len(prefix)) ~= prefix then return 0 end; "
                    + "redis.call('del', KEYS[1], KEYS[2]); "
                    + "if redis.call('get', KEYS[3]) == ARGV[1] then redis.call('del', KEYS[3]) end; "
                    + "local hourly = tonumber(redis.call('get', KEYS[4]) or '0'); "
                    + "if hourly > 1 then redis.call('decr', KEYS[4]) "
                    + "elseif hourly == 1 then redis.call('del', KEYS[4]) end; "
                    + "local daily = tonumber(redis.call('get', KEYS[5]) or '0'); "
                    + "if daily > 1 then redis.call('decr', KEYS[5]) "
                    + "elseif daily == 1 then redis.call('del', KEYS[5]) end; return 1",
            Long.class);

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('get', KEYS[1]); "
                    + "if not value then return 0 end; "
                    + "local separator = string.find(value, ':', 1, true); "
                    + "local expected = separator and string.sub(value, separator + 1) or value; "
                    + "if expected == ARGV[1] then "
                    + "redis.call('del', KEYS[1], KEYS[2]); return 1 end; "
                    + "local ttl = redis.call('pttl', KEYS[1]); "
                    + "local attempts = redis.call('incr', KEYS[2]); "
                    + "if ttl > 0 then redis.call('pexpire', KEYS[2], ttl) end; "
                    + "if attempts >= tonumber(ARGV[2]) then redis.call('del', KEYS[1], KEYS[2]) end; "
                    + "return -attempts",
            Long.class);

    private final RedisCache redisCache;
    private final VerificationProperties properties;

    public RedisSmsVerificationRepository(
            RedisCache redisCache,
            VerificationProperties properties) {
        this.redisCache = redisCache;
        this.properties = properties;
    }

    /**
     * 原子预占冷却与配额，并在调用短信供应商前保存验证码。
     */
    public Reservation reserve(
            String phoneNumber,
            String scene,
            String token,
            String code,
            Instant reservedAt,
            Duration expireTime,
            Duration cooldown,
            int hourlyLimit,
            int dailyLimit) {
        LocalDate quotaDate = reservedAt.atZone(QUOTA_ZONE).toLocalDate();
        String dailyKey = dailyQuotaKey(phoneNumber, quotaDate);
        Long result = redisCache.execute(
                RESERVE_SCRIPT,
                keys(phoneNumber, scene, quotaDate),
                token,
                code,
                expireTime.toMillis(),
                cooldown.toMillis(),
                hourlyLimit,
                HOURLY_WINDOW.toMillis(),
                dailyLimit,
                dailyTtl(reservedAt).toMillis());

        if (Objects.equals(result, RESERVED)) {
            return new Reservation(ReservationStatus.RESERVED, Duration.ZERO);
        }
        if (Objects.equals(result, COOLDOWN_REJECTED)) {
            return new Reservation(
                    ReservationStatus.COOLDOWN,
                    remaining(cooldownKey(phoneNumber, scene)));
        }
        if (Objects.equals(result, HOURLY_REJECTED)) {
            return new Reservation(
                    ReservationStatus.HOURLY_LIMIT,
                    remaining(hourlyQuotaKey(phoneNumber)));
        }
        if (Objects.equals(result, DAILY_REJECTED)) {
            return new Reservation(
                    ReservationStatus.DAILY_LIMIT,
                    remaining(dailyKey));
        }
        throw new IllegalStateException("Unexpected SMS verification reservation result: " + result);
    }

    /**
     * 仅当验证码仍属于指定预占时回滚验证码、冷却和两级配额。
     */
    public boolean rollback(
            String phoneNumber,
            String scene,
            String token,
            Instant reservedAt) {
        LocalDate quotaDate = reservedAt.atZone(QUOTA_ZONE).toLocalDate();
        Long result = redisCache.execute(
                ROLLBACK_SCRIPT,
                keys(phoneNumber, scene, quotaDate),
                token);
        return Objects.equals(result, 1L);
    }

    /**
     * 原子校验并消费验证码，错误时累计失败次数。
     */
    public VerificationResult verifyAndConsume(
            String phoneNumber,
            String scene,
            String code,
            int maxFailedAttempts) {
        Long result = redisCache.execute(
                VERIFY_SCRIPT,
                List.of(codeKey(phoneNumber, scene), attemptsKey(phoneNumber, scene)),
                code,
                maxFailedAttempts);
        if (Objects.equals(result, 1L)) {
            return new VerificationResult(true, 0);
        }
        if (result == null || result == 0L) {
            return new VerificationResult(false, 0);
        }
        if (result < 0L) {
            return new VerificationResult(false, Math.toIntExact(-result));
        }
        throw new IllegalStateException("Unexpected SMS verification result: " + result);
    }

    private List<String> keys(String phoneNumber, String scene, LocalDate quotaDate) {
        return List.of(
                codeKey(phoneNumber, scene),
                attemptsKey(phoneNumber, scene),
                cooldownKey(phoneNumber, scene),
                hourlyQuotaKey(phoneNumber),
                dailyQuotaKey(phoneNumber, quotaDate));
    }

    private Duration remaining(String key) {
        Long remaining = redisCache.getExpire(key, TimeUnit.MILLISECONDS);
        return remaining == null || remaining <= 0
                ? Duration.ZERO
                : Duration.ofMillis(remaining);
    }

    private Duration dailyTtl(Instant reservedAt) {
        Instant nextMidnight = reservedAt.atZone(QUOTA_ZONE)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(QUOTA_ZONE)
                .toInstant();
        Duration duration = Duration.between(reservedAt, nextMidnight);
        return duration.isZero() || duration.isNegative() ? Duration.ofMillis(1) : duration;
    }

    private String codeKey(String phoneNumber, String scene) {
        return key(SMS, CODE, scene, phoneNumber);
    }

    private String attemptsKey(String phoneNumber, String scene) {
        return key(SMS, ATTEMPTS, scene, phoneNumber);
    }

    private String cooldownKey(String phoneNumber, String scene) {
        return key(SMS, COOLDOWN, scene, phoneNumber);
    }

    private String hourlyQuotaKey(String phoneNumber) {
        return key(SMS, QUOTA, HOURLY, phoneNumber);
    }

    private String dailyQuotaKey(String phoneNumber, LocalDate date) {
        return key(SMS, QUOTA, DAILY, DATE_FORMATTER.format(date), phoneNumber);
    }

    private String key(String... parts) {
        String[] normalized = new String[parts.length + 1];
        normalized[0] = lowerCase(properties.getKeyPrefix());
        for (int i = 0; i < parts.length; i++) {
            normalized[i + 1] = lowerCase(parts[i]);
        }
        return RedisKeyUtil.generate(normalized);
    }

    private String lowerCase(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public enum ReservationStatus {
        RESERVED,
        COOLDOWN,
        HOURLY_LIMIT,
        DAILY_LIMIT
    }

    public record Reservation(
            ReservationStatus status,
            Duration retryAfter) {
    }

    public record VerificationResult(
            boolean verified,
            int failedAttempts) {
    }
}
