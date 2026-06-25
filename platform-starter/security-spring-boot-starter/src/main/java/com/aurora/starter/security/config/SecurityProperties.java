package com.aurora.starter.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Sa-Token 安全配置属性
 * <p>
 * 前缀: platform.security
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "platform.security")
public class SecurityProperties {

    /**
     * 是否启用安全自动配置
     */
    private boolean enabled = true;

    /**
     * Token 名称（前端请求 Header 中携带的 Key，默认 Authorization）
     */
    private String tokenName = "Authorization";

    /**
     * Token 有效期（秒），默认 7 天
     */
    private int timeout = 604800;

    /**
     * 是否打印 Sa-Token 框架内部日志
     */
    private boolean isLog = true;

    /**
     * SaInterceptor 放行路径
     */
    private List<String> excludePaths = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    );
}
