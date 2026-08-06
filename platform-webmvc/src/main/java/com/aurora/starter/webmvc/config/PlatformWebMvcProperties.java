package com.aurora.starter.webmvc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * platform-webmvc 的加密配置。
 *
 * <p>密钥值均要求为 Base64 编码、解码后恰好 32 字节的 AES-256 原始密钥。
 * 密钥由具体功能在第一次使用时懒校验。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = PlatformWebMvcProperties.PREFIX)
public class PlatformWebMvcProperties {

    public static final String PREFIX = "platform.webmvc";

    /**
     * 响应报文加密密钥。仅在接口使用 {@code @EncryptResponse} 时需要配置。
     */
    private String responseSecretKey;

    /**
     * 服务端凭据加密密钥。邮件授权码、SSH 密码等服务端凭据使用该密钥。
     */
    private String credentialSecretKey;
}
