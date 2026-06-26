package com.aurora.example.security;

import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.account.AccountTypeDefinition;
import com.aurora.starter.security.account.AccountTypeRegistry;
import com.aurora.starter.security.account.SimpleAccountTypeDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTypeRegistryTest {

    @Test
    void shouldCollectDeclaredAccounts() {
        AccountTypeDefinition admin = new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/admin/**"));
        AccountTypeDefinition merchant = new SimpleAccountTypeDefinition(AccountType.MERCHANT, List.of("/merchant/**"));

        AccountTypeRegistry registry = new AccountTypeRegistry(List.of(admin, merchant));

        assertEquals(2, registry.all().size()); // admin + merchant，不注入默认 login
        assertNotNull(registry.get(AccountType.ADMIN));
        assertNotNull(registry.get(AccountType.MERCHANT));
        assertNull(registry.get(AccountType.LOGIN)); // 业务方声明了其他账号，不注入默认 login
    }

    @Test
    void shouldInjectDefaultLoginAccountWhenAbsent() {
        AccountTypeRegistry registry = new AccountTypeRegistry(List.of());

        assertEquals(1, registry.all().size());
        assertEquals(AccountType.LOGIN, registry.get(AccountType.LOGIN).getType());
        assertTrue(registry.get(AccountType.LOGIN).getPaths().isEmpty());
    }

    @Test
    void shouldRespectUserDeclaredLoginAccount() {
        AccountTypeDefinition userLogin = new SimpleAccountTypeDefinition(AccountType.LOGIN, List.of("/**"), "user override");

        AccountTypeRegistry registry = new AccountTypeRegistry(List.of(userLogin));

        assertEquals(1, registry.all().size());
        assertEquals(List.of("/**"), registry.get(AccountType.LOGIN).getPaths());
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(NullPointerException.class, () ->
                new SimpleAccountTypeDefinition((AccountType) null, List.of())
        );
    }

    @Test
    void shouldRejectDuplicateType() {
        AccountTypeDefinition a = new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/a/**"));
        AccountTypeDefinition b = new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/b/**"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AccountTypeRegistry(List.of(a, b))
        );
        assertTrue(ex.getMessage().contains("Duplicate account type"));
        assertTrue(ex.getMessage().contains("admin"));
    }
}
