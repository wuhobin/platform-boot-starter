package com.aurora.starter.verification.redis;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.verification.config.VerificationProperties;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 邮件验证码 Redis 存储。
 */
public class RedisMailVerificationRepository {

    private static final String MAIL = "mail";
    private static final String CODE = "code";
    private static final String COOLDOWN = "cooldown";

    private static final DefaultRedisScript<Long> RELEASE_COOLDOWN_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private static final DefaultRedisScript<Long> VERIFY_AND_CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('get', KEYS[1]); "
                    + "if not value then return 0 end; "
                    + "if value ~= ARGV[1] then return -1 end; "
                    + "redis.call('del', KEYS[1]); return 1",
            Long.class);

    private static final DefaultRedisScript<Long> STORE_IF_COOLDOWN_OWNED_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[2]) ~= ARGV[1] then return 0 end; "
                    + "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); return 1",
            Long.class);

    private final RedisCache redisCache;
    private final VerificationProperties properties;

    public RedisMailVerificationRepository(
            RedisCache redisCache,
            VerificationProperties properties) {
        this.redisCache = redisCache;
        this.properties = properties;
    }

    public boolean acquireCooldown(String email, String scene, String token, Duration cooldown) {
        Boolean acquired = redisCache.setIfAbsent(
                cooldownKey(email, scene), token, cooldown.toMillis(), TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    public Duration getCooldownRemaining(String email, String scene) {
        Long remaining = redisCache.getExpire(cooldownKey(email, scene), TimeUnit.MILLISECONDS);
        return remaining == null || remaining <= 0
                ? Duration.ZERO
                : Duration.ofMillis(remaining);
    }

    public void releaseCooldown(String email, String scene, String token) {
        redisCache.execute(RELEASE_COOLDOWN_SCRIPT, List.of(cooldownKey(email, scene)), token);
    }

    public boolean storeCodeIfCooldownOwned(
            String email,
            String scene,
            String cooldownToken,
            String code,
            Duration expireTime) {
        Long result = redisCache.execute(
                STORE_IF_COOLDOWN_OWNED_SCRIPT,
                List.of(codeKey(email, scene), cooldownKey(email, scene)),
                cooldownToken,
                code,
                expireTime.toMillis());
        return Long.valueOf(1L).equals(result);
    }

    public boolean verifyAndConsume(String email, String scene, String code) {
        Long result = redisCache.execute(
                VERIFY_AND_CONSUME_SCRIPT,
                List.of(codeKey(email, scene)),
                code);
        return Long.valueOf(1L).equals(result);
    }

    private String codeKey(String email, String scene) {
        return RedisKeyUtil.generate(
                lowerCase(properties.getKeyPrefix()),
                MAIL,
                CODE,
                lowerCase(scene),
                lowerCase(email));
    }

    private String cooldownKey(String email, String scene) {
        return RedisKeyUtil.generate(
                lowerCase(properties.getKeyPrefix()),
                MAIL,
                COOLDOWN,
                lowerCase(scene),
                lowerCase(email));
    }

    private String lowerCase(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
