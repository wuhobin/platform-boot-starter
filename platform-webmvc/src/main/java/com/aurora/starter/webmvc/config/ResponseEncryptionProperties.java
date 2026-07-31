package com.aurora.starter.webmvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口响应加密配置。
 */
@Data
@ConfigurationProperties(prefix = ResponseEncryptionProperties.PREFIX)
public class ResponseEncryptionProperties {

    public static final String PREFIX = "platform.webmvc.response-encryption";

    /**
     * 是否启用接口响应加密，默认关闭。
     */
    private boolean enabled = false;

    /**
     * Base64 编码的 32 字节 AES 密钥。
     */
    private String key;
}
