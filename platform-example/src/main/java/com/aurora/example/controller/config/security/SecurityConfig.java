package com.aurora.example.controller.config.security;

import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.account.AccountTypeDefinition;
import com.aurora.starter.security.account.SimpleAccountTypeDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 声明多账号体系：
 * <ul>
 *   <li>admin —— 后台管理员，paths={@code /admin/**}</li>
 *   <li>merchant —— 商家，paths={@code /merchant/**}</li>
 * </ul>
 * <p>声明后 starter 进入"显式多账号模式"，仅校验各账号 paths 命中的 URL。</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public AccountTypeDefinition adminAccount() {
        return new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/admin/**"), "后台管理员");
    }

    @Bean
    public AccountTypeDefinition merchantAccount() {
        return new SimpleAccountTypeDefinition(AccountType.MERCHANT, List.of("/merchant/**"), "商家");
    }
}
