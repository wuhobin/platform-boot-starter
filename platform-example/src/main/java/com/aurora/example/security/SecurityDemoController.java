package com.aurora.example.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.domain.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class SecurityDemoController {

    /**
     * 登录接口（使用固定的 demo 用户）
     */
    @PostMapping("/login")
    public Result<?> login() {
        // Demo: 固定用户 ID=1
        SecurityUtils.login(1L);
        return Result.data(SecurityUtils.getTokenInfo());
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        SecurityUtils.logout();
        return Result.success("已登出");
    }

    /**
     * 获取当前登录信息
     */
    @GetMapping("/info")
    public Result<?> info() {
        Map<String, Object> data = Map.of(
                "loginId", SecurityUtils.getLoginId(),
                "isLogin", SecurityUtils.isLogin(),
                "hasUserRead", SecurityUtils.hasPermission("user:read"),
                "hasAdminWrite", SecurityUtils.hasPermission("admin:write"),
                "hasRoleUser", SecurityUtils.hasRole("user"),
                "hasRoleAdmin", SecurityUtils.hasRole("admin")
        );
        return Result.data(data);
    }

    /**
     * 需要 admin:write 权限（此 demo 用户没有该权限，会返回 403）
     */
    @SaCheckPermission("admin:write")
    @GetMapping("/admin-only")
    public Result<?> adminOnly() {
        return Result.success("你有 admin:write 权限");
    }

    /**
     * 需要 admin 角色（此 demo 用户没有该角色，会返回 403）
     */
    @SaCheckRole("admin")
    @GetMapping("/admin-role")
    public Result<?> adminRole() {
        return Result.success("你有 admin 角色");
    }
}
