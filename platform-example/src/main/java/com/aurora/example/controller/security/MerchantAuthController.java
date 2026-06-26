package com.aurora.example.controller.security;

import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.domain.response.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchant/auth")
public class MerchantAuthController {

    @PostMapping("/login")
    public Result<?> login() {
        // Demo: 固定商家 ID=20001
        SecurityUtils.loginAs(AccountType.MERCHANT, 20001L);
        return Result.data(SecurityUtils.getTokenInfoAs(AccountType.MERCHANT));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        SecurityUtils.logoutAs(AccountType.MERCHANT);
        return Result.success("merchant 已登出");
    }
}