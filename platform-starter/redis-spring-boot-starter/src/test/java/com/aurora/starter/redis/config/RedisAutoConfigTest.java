package com.aurora.starter.redis.config;

import com.aurora.starter.redis.core.JsonRedisTemplate;
import com.aurora.starter.redis.core.RedisCache;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfig.class))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class));

    @Test
    void shouldUseDedicatedJsonTemplateWhenDefaultRedisTemplateExists() {
        RedisTemplate<?, ?> defaultRedisTemplate = mock(RedisTemplate.class);

        contextRunner
                .withBean("redisTemplate", RedisTemplate.class, () -> defaultRedisTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(JsonRedisTemplate.class);
                    assertThat(context).hasBean("jsonRedisTemplate");

                    JsonRedisTemplate jsonRedisTemplate = context.getBean(
                            "jsonRedisTemplate", JsonRedisTemplate.class);
                    RedisCache redisCache = context.getBean(RedisCache.class);

                    assertThat(context.getBean("redisTemplate")).isSameAs(defaultRedisTemplate);
                    assertThat(jsonRedisTemplate).isNotSameAs(defaultRedisTemplate);
                    assertThat(new String(
                            ((GenericJackson2JsonRedisSerializer) jsonRedisTemplate
                                    .getValueSerializer()).serialize(300_000L),
                            StandardCharsets.UTF_8))
                            .isEqualTo("300000");
                    assertThat(ReflectionTestUtils.getField(redisCache, "redisTemplate"))
                            .isSameAs(jsonRedisTemplate);
                });
    }

    @Test
    void shouldBackOffForNamedJsonTemplateOverride() {
        JsonRedisTemplate override = mock(JsonRedisTemplate.class);

        contextRunner
                .withBean("jsonRedisTemplate", JsonRedisTemplate.class, () -> override)
                .run(context -> {
                    assertThat(context).hasSingleBean(JsonRedisTemplate.class);
                    assertThat(context.getBean("jsonRedisTemplate")).isSameAs(override);
                    assertThat(ReflectionTestUtils.getField(
                            context.getBean(RedisCache.class), "redisTemplate"))
                            .isSameAs(override);
                });
    }

    @Test
    void shouldRoundTripJavaTimeValues() {
        contextRunner.run(context -> {
            JsonRedisTemplate jsonRedisTemplate = context.getBean(JsonRedisTemplate.class);
            GenericJackson2JsonRedisSerializer serializer =
                    (GenericJackson2JsonRedisSerializer) jsonRedisTemplate.getValueSerializer();
            DateTimeCacheValue value = new DateTimeCacheValue(
                    LocalDateTime.of(2026, 8, 5, 13, 30, 45, 123_000_000));

            Object restored = serializer.deserialize(serializer.serialize(value));

            assertThat(restored).isEqualTo(value);
        });
    }

    private record DateTimeCacheValue(LocalDateTime loginTime) {
    }
}
