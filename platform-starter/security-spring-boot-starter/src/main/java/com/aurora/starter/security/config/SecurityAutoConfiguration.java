package com.aurora.starter.security.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.aurora.starter.security.spi.PermissionProvider;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Sa-Token 安全自动配置
 * <p>
 * 当 platform.security.enabled=true 时生效（默认开启）。
 * 自动注册 SaTokenDao（Redisson）、StpInterface（PermissionProvider 适配）、SaInterceptor。
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;
    private final ObjectProvider<PermissionProvider> permissionProvider;
    /**
     * excludePaths 缓存为数组，避免每次请求都重新分配。
     */
    private final String[] excludePathArray;

    public SecurityAutoConfiguration(SecurityProperties securityProperties,
                                     ObjectProvider<PermissionProvider> permissionProvider) {
        this.securityProperties = securityProperties;
        this.permissionProvider = permissionProvider;
        this.excludePathArray = securityProperties.getExcludePaths().toArray(new String[0]);
    }

    /**
     * 配置 Sa-Token 参数，使用 @Primary 覆盖 Sa-Token 自身的 SaTokenConfig bean
     * <p>
     * 默认配置遵循 RESTful Bearer Token 规范：
     * Header: Authorization: Bearer &lt;token&gt;
     * 前后端分离，仅从 Header 读取，不从 Cookie/Body 读取。
     * </p>
     */
    @Primary
    @Bean
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        // Token 标识名（前端请求 Header 中需要携带的 Key）
        config.setTokenName(securityProperties.getTokenName());
        // Token 前缀（遵循 Bearer Token 标准规范，注意后面有一个空格）
        config.setTokenPrefix("Bearer");
        // Token 有效期（单位：秒），默认 7 天
        config.setTimeout(securityProperties.getTimeout());
        // 临时有效期（指定时间内无操作则半途失效，-1 代表不开启）
        config.setActiveTimeout(-1);
        // 是否允许同一账号多端同时登录
        config.setIsConcurrent(true);
        // 在多人登录同一账号时，是否共享同一个 Token
        config.setIsShare(true);
        // Token 生成风格，默认 uuid 格式
        config.setTokenStyle(securityProperties.getTokenStyle());
        // 是否向控制台打印框架内部日志
        config.setIsLog(securityProperties.isLog());
        // 是否打印每次请求的 Token 信息
        config.setIsPrint(false);
        // 是否从 Cookie 中尝试读取 Token
        config.setIsReadCookie(false);
        // 是否从 Header 中读取 Token（前后端分离项目必须开启）
        config.setIsReadHeader(true);
        // 是否从 Body 请求体中读取 Token
        config.setIsReadBody(false);
        return config;
    }

    /**
     * 注册 SaTokenDao（Redisson 实现）
     * <p>
     * 复用已存在的 RedissonClient Bean（由 redisson-spring-boot-starter 或 redis-spring-boot-starter 提供）。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao(RedissonClient redissonClient) {
        return new SaTokenDaoForRedisson(redissonClient);
    }

    /**
     * 注册 StpInterface 实现（适配 PermissionProvider SPI）
     */
    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface() {
        return new PermissionProviderBackedStpInterface(permissionProvider);
    }

    /**
     * 注册 SaInterceptor 路由拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter
                    .match("/**")
                    .notMatch(excludePathArray)
                    .check(r -> StpUtil.checkLogin());
        }))
        // 同时在 Spring MVC 层面排除，避免不必要地进入 SaInterceptor
        .excludePathPatterns("/doc.html", "/doc.html/**",
                "/webjars/**",
                "/swagger-resources/**", "/swagger-ui/**", "/swagger-ui.html",
                "/v3/api-docs/**", "/v2/api-docs/**",
                "/actuator/**", "/actuator",
                "/favicon.ico", "/favicon.ico/**",
                "/error")
        .addPathPatterns("/**");
    }

    /**
     * 基于 {@link PermissionProvider} 的 StpInterface 实现。
     * <p>
     * 每次鉴权时通过 {@link ObjectProvider} 解析（避免构造时强制要求存在），
     * 解决匿名内部类在 Sa-Token 启动日志中显示为 {@code null} 的问题。
     * </p>
     */
    private static final class PermissionProviderBackedStpInterface implements StpInterface {

        private final ObjectProvider<PermissionProvider> provider;

        private PermissionProviderBackedStpInterface(ObjectProvider<PermissionProvider> provider) {
            this.provider = provider;
        }

        private PermissionProvider resolve() {
            return provider.getIfAvailable();
        }

        @Override
        public List<String> getPermissionList(Object loginId, String loginType) {
            PermissionProvider p = resolve();
            return p != null ? p.getPermissionList(loginId, loginType) : List.of();
        }

        @Override
        public List<String> getRoleList(Object loginId, String loginType) {
            PermissionProvider p = resolve();
            return p != null ? p.getRoleList(loginId, loginType) : List.of();
        }

        @Override
        public String toString() {
            PermissionProvider p = resolve();
            return p != null ? p.getClass().getSimpleName() : "PermissionProvider (not provided)";
        }
    }
}
