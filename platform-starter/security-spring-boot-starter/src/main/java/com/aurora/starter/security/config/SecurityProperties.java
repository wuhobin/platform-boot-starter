package com.aurora.starter.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.stream.Stream;

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
     * starter 自身保证已知的放行路径（默认开放），最终放行列表 = 默认列表 + 用户自定义列表（去重）
     */
    private static final List<String> DEFAULT_EXCLUDE_PATHS = List.of(
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
     * 业务方自定义的放行路径（与 starter 默认放行路径合并去重）
     * <p>
     * 最终放行列表 = starter 默认列表 + 本字段列表（去重）。
     * 业务登录/注册等接口直接追加到 yml 即可，无需重复写 swagger/actuator 等默认路径。
     * </p>
     */
    private List<String> excludePaths = List.of();

    /**
     * 自定义 setter：将用户配置的 excludePaths 与默认列表合并去重
     */
    public void setExcludePaths(List<String> userPaths) {
        if (userPaths == null || userPaths.isEmpty()) {
            this.excludePaths = DEFAULT_EXCLUDE_PATHS;
        } else {
            this.excludePaths = Stream.concat(DEFAULT_EXCLUDE_PATHS.stream(), userPaths.stream())
                    .distinct()
                    .toList();
        }
    }

    /**
     * 暴露默认列表给其他模块使用（例如调试日志）
     */
    public static List<String> getDefaultExcludePaths() {
        return DEFAULT_EXCLUDE_PATHS;
    }
}
