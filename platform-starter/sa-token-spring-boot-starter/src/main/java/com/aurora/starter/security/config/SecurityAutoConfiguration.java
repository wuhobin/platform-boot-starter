package com.aurora.starter.security.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.account.AccountTypeDefinition;
import com.aurora.starter.security.account.AccountTypeRegistry;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.security.log.SaLogForSlf4j;
import com.aurora.starter.security.spi.PermissionProvider;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

/**
 * Sa-Token 安全自动配置
 * <p>
 * 当 {@code platform.security.enabled=true} 时生效（默认开启）。
 * 自动注册：
 * <ul>
 *   <li>{@link SaTokenConfig}（Bearer Token 风格，token-name/timeout 共享给所有账号）</li>
 *   <li>{@link SaTokenDao}（Redisson）</li>
 *   <li>{@link StpInterface}（{@link PermissionProvider} 适配，{@code loginType} 透传）</li>
 *   <li>{@link SaInterceptor}（单拦截器 + SaRouter 多 match，按 {@link AccountTypeRegistry} 路由）</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SecurityProperties securityProperties;
    private final ObjectProvider<PermissionProvider> permissionProvider;
    /**
     * 内部使用：从 {@link List} 构造的 {@link AccountTypeRegistry} 实例。
     * <p>不暴露为 Spring Bean —— 它是本自动配置类的实现细节，外部无需注入。
     * 业务方可通过 {@link AccountTypeDefinition} 间接影响（声明 Bean）。</p>
     */
    private final AccountTypeRegistry accountRegistry;
    private final boolean hasExplicitAccounts;

    public SecurityAutoConfiguration(SecurityProperties securityProperties,
                                     ObjectProvider<PermissionProvider> permissionProvider,
                                     List<AccountTypeDefinition> accountDefinitions) {
        this.securityProperties = securityProperties;
        this.permissionProvider = permissionProvider;
        this.accountRegistry = new AccountTypeRegistry(accountDefinitions);
        this.hasExplicitAccounts = accountDefinitions.stream()
                .anyMatch(a -> a.getType() != AccountType.LOGIN);
        if (hasExplicitAccounts) {
            SecurityUtils.markMultiAccountMode();
        }
    }

    @PostConstruct
    void logStartupSummary() {
        log.info("{}", buildStartupSummary());
    }

    String buildStartupSummary() {
        PermissionProvider p = permissionProvider.getIfAvailable();
        String lineSeparator = System.lineSeparator();
        String accounts = accountRegistry.all().stream()
                .map(a -> String.format(lineSeparator + "      ├─ %-8s (%s, paths=%s)",
                        a.getType().getCode(),
                        a.getDescription().isBlank() ? "-" : a.getDescription(),
                        a.getPaths()))
                .collect(Collectors.joining());
        return "Platform Security Starter initialized"
                + lineSeparator + "    Token         : " + securityProperties.getTokenName()
                + " Bearer <token>, timeout=" + securityProperties.getTimeout() + "s"
                + lineSeparator + "    Multi-Account : " + (hasExplicitAccounts
                ? "ON（多账号模式）" : "OFF（单账号模式，catch-all /**）")
                + lineSeparator + "    Accounts      : " + accountRegistry.all().size() + accounts
                + lineSeparator + "    Permission SPI: "
                + (p != null ? p.getClass().getSimpleName() : "(not provided)")
                + lineSeparator + "    Excludes      : " + securityProperties.getExcludePaths();
    }

    /**
     * 配置 Sa-Token 参数，使用 @Primary 覆盖 Sa-Token 自身的 SaTokenConfig bean。
     * <p>所有账号共享 token-name/timeout；账号隔离由 sa-token loginType 机制保证。</p>
     */
    @Primary
    @Bean
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName(securityProperties.getTokenName());
        config.setTokenPrefix("Bearer");
        config.setTimeout(securityProperties.getTimeout());
        config.setActiveTimeout(-1);
        config.setIsConcurrent(true);
        config.setIsShare(true);
        config.setTokenStyle(securityProperties.getTokenStyle());
        config.setIsLog(securityProperties.isLog());
        config.setIsPrint(false);
        config.setIsReadCookie(false);
        config.setIsReadHeader(true);
        config.setIsReadBody(false);
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao(RedissonClient redissonClient) {
        return new SaTokenDaoForRedisson(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface() {
        return new PermissionProviderBackedStpInterface(permissionProvider);
    }

    @Bean
    public SaLogForSlf4j saLogForSlf4j() {
        return new SaLogForSlf4j();
    }

    /**
     * 注册 SaInterceptor 路由拦截器。
     * <p>
     * <b>模式判定：</b>当注册表中存在除 {@link AccountType#LOGIN} 以外的账号时，
     * 认为业务方声明了多账号体系，每个账号按自己的 paths 路由；
     * 否则保持旧 catch-all {@code /**} 行为（向后兼容）。
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] defaultExcludes = securityProperties.getExcludePaths().toArray(new String[0]);

        registry.addInterceptor(new SaInterceptor(handler -> {
            if (!hasExplicitAccounts) {
                // 单账号世界：保持旧 catch-all 行为
                SaRouter.match("/**")
                        .notMatch(defaultExcludes)
                        .check(r -> StpUtil.checkLogin());
                return;
            }
            // 多账号世界：每个账号按自己的 paths 路由
            for (AccountTypeDefinition acc : accountRegistry.all()) {
                List<String> paths = acc.getPaths();
                if (paths == null || paths.isEmpty()) {
                    continue;
                }
                SaRouter.match(paths.toArray(new String[0]))
                        .notMatch(defaultExcludes)
                        .check(r -> SecurityUtils.checkLoginAs(acc.getType()));
            }
        })).addPathPatterns("/**");
    }

    /**
     * 基于 {@link PermissionProvider} 的 StpInterface 实现。
     * <p>{@code loginType} 透传给业务方 PermissionProvider，由其在内部 switch 分派。</p>
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
            if (p == null) {
                return List.of();
            }
            AccountType type = AccountType.fromCode(loginType);
            if (type == null) {
                return List.of();
            }
            return p.getPermissionList(loginId, type);
        }

        @Override
        public List<String> getRoleList(Object loginId, String loginType) {
            PermissionProvider p = resolve();
            if (p == null) {
                return List.of();
            }
            AccountType type = AccountType.fromCode(loginType);
            if (type == null) {
                return List.of();
            }
            return p.getRoleList(loginId, type);
        }

        @Override
        public String toString() {
            PermissionProvider p = resolve();
            return p != null ? p.getClass().getSimpleName() : "PermissionProvider (not provided)";
        }
    }
}
