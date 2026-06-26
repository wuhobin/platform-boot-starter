package com.aurora.example.controller.security;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.aurora.starter.webmvc.domain.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchant/orders")
@SaCheckLogin(type = "merchant")   // 类级注解：所有方法都需要 merchant 登录态
public class MerchantOrderController {

    @GetMapping
    public Result<?> list() {
        return Result.success("商家订单列表 - 需 merchant 登录态");
    }
}