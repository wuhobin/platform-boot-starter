package com.aurora.starter.security.account;

import java.util.List;
import java.util.Objects;

/**
 * {@link AccountTypeDefinition} 的简单 record 实现。
 * <p>业务方最常用：</p>
 * <pre>{@code
 * @Bean
 * public AccountTypeDefinition adminAccount() {
 *     return new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/admin/**"), "后台管理员");
 * }
 * }</pre>
 */
public record SimpleAccountTypeDefinition(
        AccountType type,
        List<String> paths,
        String description
) implements AccountTypeDefinition {

    /**
     * 紧凑构造器：防御 null type 与 null description
     * （接口默认方法返回 {@code ""}，但 record 的 {@code getDescription()} 直接访问字段）。
     */
    public SimpleAccountTypeDefinition {
        Objects.requireNonNull(type, "type must not be null");
        if (description == null) {
            description = "";
        }
    }

    public SimpleAccountTypeDefinition(AccountType type, List<String> paths) {
        this(type, paths, "");
    }

    @Override
    public AccountType getType() {
        return type;
    }

    @Override
    public List<String> getPaths() {
        return paths;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
