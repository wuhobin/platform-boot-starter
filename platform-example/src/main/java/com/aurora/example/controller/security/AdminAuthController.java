package com.aurora.example.controller.security;

import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.domain.response.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    @PostMapping("/login")
    public Result<?> login() {
        // Demo: 固定管理员 ID=10001
        SecurityUtils.loginAs(AccountType.ADMIN, 10001L);
        return Result.data(SecurityUtils.getTokenInfoAs(AccountType.ADMIN));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        SecurityUtils.logoutAs(AccountType.ADMIN);
        return Result.success("admin 已登出");
    }
}