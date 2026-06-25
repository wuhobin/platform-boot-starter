package com.aurora.example.security;

import com.aurora.starter.security.spi.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyPermissionProvider implements PermissionProvider {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // Demo: 所有用户都拥有 user:read 权限
        return List.of("user:read");
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // Demo: 所有用户都是 user 角色
        return List.of("user");
    }
}
