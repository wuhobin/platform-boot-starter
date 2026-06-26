package com.aurora.starter.security.account;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * starter 内部组件，收集所有 {@link AccountTypeDefinition} Bean 并做唯一性校验。
 * <p>
 * 启动期完成：
 * <ul>
 *   <li>校验所有 type 全局唯一（重复时 fail-fast）</li>
 *   <li>若业务方未声明任何账号 Bean，注入默认 {@link AccountType#LOGIN}（无 paths），
 *       保留单账号 catch-all 行为</li>
 *   <li>若业务方声明了账号 Bean，则<b>不注入默认 login</b>，完全交给业务方控制</li>
 * </ul>
 * </p>
 * <p>
 * <b>为何不标 {@code @Component}：</b>本类由 {@code SecurityAutoConfiguration} 通过
 * {@code @Bean} 工厂方法创建。这样做的好处是 starter 内部包
 * （{@code com.aurora.starter.security.*}）无需被业务方加入到组件扫描路径中，
 * 符合 Spring Boot starter 的"零配置"原则。
 * </p>
 */
public class AccountTypeRegistry {

    private final Map<AccountType, AccountTypeDefinition> accounts;

    public AccountTypeRegistry(List<AccountTypeDefinition> beans) {
        Map<AccountType, AccountTypeDefinition> map = new LinkedHashMap<>();
        for (AccountTypeDefinition def : beans) {
            AccountType type = def.getType();
            AccountTypeDefinition prev = map.put(type, def);
            if (prev != null) {
                throw new IllegalStateException(
                        "Duplicate account type: '" + type.getCode() + "', beans=["
                                + prev.getClass().getName() + ", " + def.getClass().getName() + "]");
            }
        }
        // 仅当业务方未声明任何账号时才注入默认 login（无 paths），
        // 保留单账号 catch-all 行为。一旦业务方声明了多账号，完全由业务方控制，
        // 不再夹带默认 login 造成混淆。
        if (beans.isEmpty()) {
            map.put(AccountType.LOGIN,
                    new SimpleAccountTypeDefinition(AccountType.LOGIN, List.of(), "default"));
        }
        this.accounts = map;
    }

    /**
     * 返回所有账号（含默认 login），按 type code 排序。
     */
    public List<AccountTypeDefinition> all() {
        return accounts.values().stream()
                .sorted(Comparator.comparing(def -> def.getType().getCode()))
                .toList();
    }

    /**
     * 按 type 查找账号，未找到返回 {@code null}。
     */
    public AccountTypeDefinition get(AccountType type) {
        return accounts.get(type);
    }
}
