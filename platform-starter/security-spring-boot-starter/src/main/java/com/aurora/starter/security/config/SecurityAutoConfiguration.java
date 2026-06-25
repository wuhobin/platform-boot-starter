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
    private final PermissionProvider permissionProvider;

    public SecurityAutoConfiguration(SecurityProperties securityProperties,
                                     ObjectProvider<PermissionProvider> permissionProvider) {
        this.securityProperties = securityProperties;
        this.permissionProvider = permissionProvider.getIfAvailable();
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
        config.setTokenName(securityProperties.getTokenName());
        config.setTokenPrefix("Bearer");
        config.setTimeout(securityProperties.getTimeout());
        config.setActivityTimeout(-1);
        config.setIsConcurrent(true);
        config.setIsShare(true);
        config.setTokenStyle("uuid");
        config.setIsLog(securityProperties.isLog());
        config.setIsPrint(false);
        config.setIsReadCookie(false);
        config.setIsReadHeader(true);
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
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                if (permissionProvider != null) {
                    return permissionProvider.getPermissionList(loginId, loginType);
                }
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                if (permissionProvider != null) {
                    return permissionProvider.getRoleList(loginId, loginType);
                }
                return List.of();
            }
        };
    }

    /**
     * 注册 SaInterceptor 路由拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter
                    .match("/**")
                    .notMatch(securityProperties.getExcludePaths().toArray(new String[0]))
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
