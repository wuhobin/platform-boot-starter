package com.aurora.starter.redis.core;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;

/**
 * jack json序列化
 */
public class JsonRedisTemplate extends RedisTemplate<String, Object> {

    private static final RedisSerializer<String> STRING_REDIS_SERIALIZER = new StringRedisSerializer();

    private static final RedisSerializer<Object> JACKSON_2_JSON_REDIS_SERIALIZER = new GenericJackson2JsonRedisSerializer();

    public JsonRedisTemplate(final RedisConnectionFactory connectionFactory) {
        Objects.requireNonNull(connectionFactory, "connectionFactory 不能为null");

        this.setKeySerializer(STRING_REDIS_SERIALIZER);
        this.setValueSerializer(JACKSON_2_JSON_REDIS_SERIALIZER);

        this.setHashKeySerializer(STRING_REDIS_SERIALIZER);
        this.setHashValueSerializer(JACKSON_2_JSON_REDIS_SERIALIZER);

        setConnectionFactory(connectionFactory);
        afterPropertiesSet();
    }
}
