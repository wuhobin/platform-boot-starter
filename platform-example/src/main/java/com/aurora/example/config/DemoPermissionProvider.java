package com.aurora.example.config;

import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.spi.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo 权限/角色提供者。
 * <p>实际生产中应根据 {@code loginId} + {@code loginType} 查数据库或调用权限服务。</p>
 */
@Component
public class DemoPermissionProvider implements PermissionProvider {

    @Override
    public List<String> getPermissionList(Object loginId, AccountType loginType) {
        return switch (loginType) {
            case ADMIN    -> List.of("user:add", "user:edit", "user:delete");
            case MERCHANT -> List.of("order:view", "order:ship");
            default       -> List.of();
        };
    }

    @Override
    public List<String> getRoleList(Object loginId, AccountType loginType) {
        return switch (loginType) {
            case ADMIN    -> List.of("ADMIN");
            case MERCHANT -> List.of("MERCHANT");
            default       -> List.of("USER");
        };
    }
}
