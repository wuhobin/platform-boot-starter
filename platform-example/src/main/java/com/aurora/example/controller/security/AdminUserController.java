package com.aurora.example.controller.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.webmvc.domain.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @GetMapping
    public Result<?> list() {
        return Result.success("admin 用户列表 - 需 admin 登录态");
    }

    /**
     * 方法级注解鉴权：admin 账号 + user:add 权限。
     */
    @SaCheckPermission(type = "admin", value = "user:add")
    @PostMapping
    public Result<?> create() {
        return Result.success("创建用户 - 需要 admin 的 user:add 权限");
    }
}