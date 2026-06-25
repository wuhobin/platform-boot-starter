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
     * Token 生成风格（uuid / simple-uuid / random-32 / random-64 / random-128 / tik）
     */
    private String tokenStyle = "uuid";

    /**
     * 是否打印 Sa-Token 框架内部日志
     */
    private boolean isLog = false;

    /**
     * SaInterceptor 放行路径
     * <p>
     * 默认包含 Knife4j/swagger 文档、Actuator、Spring Boot 错误页等开放访问的路径。
     * 业务方应根据实际接口扩展该列表 —— 业务登录/注册等路径由消费方提供。
     * </p>
     */
    private List<String> excludePaths = List.of(
            // Knife4j / Swagger UI
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // OpenAPI 文档
            "/v3/api-docs/**",
            "/v2/api-docs/**",
            // Spring Boot Actuator
            "/actuator/**",
            // 静态资源
            "/favicon.ico",
            // Spring Boot 错误页（避免 401 重定向循环）
            "/error"
    );
}
