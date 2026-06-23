package com.aurora.starter.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 布隆过滤器配置属性.
 * <p>
 * 配置前缀: platform.redis.bloom-filter
 * enabled 默认为 false，不配置不启用。
 *
 * @author whb
 */
@Data
@Validated
@ConfigurationProperties(prefix = "platform.redis.bloom-filter")
public class BloomFilterProperties {

    /** 总开关，默认 false */
    private boolean enabled = false;

    /** 布隆过滤器定义列表 */
    @Valid
    private List<FilterConfig> filters = new ArrayList<>();

    @Data
    public static class FilterConfig {

        /** Redis key 名（必填） */
        @NotBlank
        private String name;

        /** 预期插入元素数量（必填） */
        @NotNull
        private Long expectedInsertions;

        /** 期望误判率，0 < value < 1（必填） */
        @NotNull
        private Double falsePositiveProbability;
    }
}
