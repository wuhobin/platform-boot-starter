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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

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
    private final SaTokenConfig saTokenConfig;

    public SecurityAutoConfiguration(SecurityProperties securityProperties,
                                     ObjectProvider<PermissionProvider> permissionProvider,
                                     SaTokenConfig saTokenConfig) {
        this.securityProperties = securityProperties;
        this.permissionProvider = permissionProvider.getIfAvailable();
        this.saTokenConfig = saTokenConfig;
    }

    /**
     * 在 SaTokenConfig bean 初始化后，用 platform.security.* 覆盖默认配置
     * <p>
     * Sa-Token 自身的 SaBeanRegister 已注册了 SaTokenConfig bean（从 sa-token.* 配置读取），
     * 这里在其基础上追加/覆盖默认值，遵循 RESTful Bearer Token 规范。
     * 如果业务方在 yml 中显式配置了 sa-token.* 属性，SaBeanRegister 已将其绑定到 SaTokenConfig，
     * 此处的设置会被 yml 配置覆盖（Spring 属性优先级更高）。
     * </p>
     */
    @PostConstruct
    public void customizeSaTokenConfig() {
        saTokenConfig.setTokenName(securityProperties.getTokenName());
        saTokenConfig.setTokenPrefix("Bearer");
        saTokenConfig.setTimeout(securityProperties.getTimeout());
        saTokenConfig.setActivityTimeout(-1);
        saTokenConfig.setIsConcurrent(true);
        saTokenConfig.setIsShare(true);
        saTokenConfig.setTokenStyle("uuid");
        saTokenConfig.setIsLog(securityProperties.isLog());
        saTokenConfig.setIsPrint(false);
        saTokenConfig.setIsReadCookie(false);
        saTokenConfig.setIsReadHeader(true);
        saTokenConfig.setIsReadBody(false);
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
