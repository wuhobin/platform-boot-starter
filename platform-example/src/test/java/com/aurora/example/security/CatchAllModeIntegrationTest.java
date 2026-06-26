package com.aurora.example.security;

import com.aurora.starter.security.account.AccountTypeRegistry;
import com.aurora.starter.security.account.SimpleAccountTypeDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单账号模式（业务方未声明 AccountTypeDefinition）的向后兼容验证。
 * <p>
 * 不启动 Spring 上下文——直接用单元测试验证核心行为：
 * <ul>
 *   <li>空 Bean 注入 → AccountTypeRegistry 自动兜底一个无 paths 的 LOGIN 账号</li>
 * </ul>
 * 路由层的 catch-all 行为（{@code SaRouter.match("/**").check(StpUtil.checkLogin())}）
 * 未在此验证——那是旧版原有代码，属回归测试范畴，本 PR 的新增逻辑不修改它。
 * </p>
 */
@DisplayName("单账号模式")
class SingleAccountModeTest {

    @Test
    @DisplayName("未声明任何 AccountTypeDefinition → 注册表仅含兜底 LOGIN")
    void shouldInjectDefaultLoginWhenNoBeansDeclared() {
        AccountTypeRegistry registry = new AccountTypeRegistry(List.of());

        assertEquals(1, registry.all().size());
        assertNotNull(registry.get(com.aurora.starter.security.account.AccountType.LOGIN));
        assertTrue(registry.get(com.aurora.starter.security.account.AccountType.LOGIN).getPaths().isEmpty());
    }
}
