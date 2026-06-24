package com.aurora.starter.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 两级缓存配置属性.
 * <p>
 * 配置前缀: platform.redis.two-level-cache
 * enabled 默认为 false，不配置不启用。
 *
 * @author whb
 */
@Data
@ConfigurationProperties(prefix = "platform.redis.two-level-cache")
public class TwoLevelCacheProperties {

    /** 总开关，默认 false */
    private boolean enabled = false;

    /** Caffeine 全局默认最大条目数，默认 10000 */
    private long maxSize = 10000;

    /** 全局默认 L1 TTL，默认 300 秒 */
    private Duration defaultTtl = Duration.ofSeconds(300);

    /** 差异化配置的命名实例 */
    @Valid
    private List<InstanceConfig> instances = new ArrayList<>();

    @Data
    public static class InstanceConfig {

        /** 实例唯一标识（必填） */
        @NotBlank
        private String name;

        /** Caffeine 最大条目数，不配则继承全局 maxSize */
        private Long maxSize;

        /** L1 默认 TTL，不配则继承全局 defaultTtl */
        private Duration defaultTtl;
    }
}
