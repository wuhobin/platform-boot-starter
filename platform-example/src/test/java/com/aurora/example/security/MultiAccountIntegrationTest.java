package com.aurora.example.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 多账号体系集成测试，验证：
 * <ul>
 *   <li>/admin/** 路由到 admin 登录态校验</li>
 *   <li>/merchant/** 路由到 merchant 登录态校验</li>
 *   <li>merchant token 无法访问 /admin/**（账号隔离）</li>
 *   <li>路由层先于注解层拦截（merchant token 访问 admin-only 资源返回 401 而非 403）</li>
 * </ul>
 *
 * <p><b>关于 HTTP 200 + Result.code：</b>本工程 webmvc 通用件的
 * {@code SaTokenExceptionHandler} 将 sa-token 异常转换为
 * HTTP 200 + {@code Result.code = 401/403} 的统一返回结构，
 * 因此本测试断言 {@code $.code} 而非 HTTP status。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("多账号体系集成测试")
class MultiAccountIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("merchant 登录后可访问 /merchant/orders（merchant happy path）")
    void merchantLoginThenAccess() throws Exception {
        MvcResult loginResult = mvc.perform(post("/merchant/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String merchantAuth = authHeader(extractTokenValue(loginResult.getResponse().getContentAsString()));

        mvc.perform(get("/merchant/orders").header("Authorization", merchantAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("未登录访问 /admin/users 返回 code=401（多账号模式拦截）")
    void unauthenticatedAdminAccess() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("admin 登录后可访问 /admin/users（携带 token 时 code=200）")
    void adminLoginThenAccess() throws Exception {
        // admin 登录：从返回 data.tokenName + data.tokenValue 中提取 token
        MvcResult loginResult = mvc.perform(post("/admin/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String loginBody = loginResult.getResponse().getContentAsString();
        String tokenValue = extractTokenValue(loginBody);

        // 携带 admin token 访问 /admin/users 应放行
        mvc.perform(get("/admin/users").header("Authorization", authHeader(tokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("merchant 登录后无法访问 /admin/**（账号隔离）")
    void merchantTokenCannotAccessAdmin() throws Exception {
        // merchant 登录并提取 token
        MvcResult loginResult = mvc.perform(post("/merchant/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String merchantAuth = authHeader(extractTokenValue(loginResult.getResponse().getContentAsString()));

        // 用 merchant token 访问 /admin/users 应被 admin namespace 拦截 → code=401
        mvc.perform(get("/admin/users").header("Authorization", merchantAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("@SaCheckPermission(type=\"admin\") 拦截：merchant token 无 user:add 权限 → code=401（路由层先于注解层拦截）")
    void annotationCheckPermissionType() throws Exception {
        // merchant 登录（但只有 order:* 权限）
        MvcResult loginResult = mvc.perform(post("/merchant/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String merchantAuth = authHeader(extractTokenValue(loginResult.getResponse().getContentAsString()));

        // 用 merchant token 访问 /admin/users POST（需要 admin 的 user:add 权限）
        // 但 /admin/** 的 SaRouter 路由会先校验 admin 登录——merchant token 不算 admin 登录
        // 所以这一步会返回 code=401（未登录拦截），而不是 code=403（注解层权限不足）
        mvc.perform(post("/admin/users").header("Authorization", merchantAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 从 sa-token 登录响应 JSON 中提取 {@code data.tokenValue}。
     * <p>响应结构：{@code {"code":200,"msg":"...","data":{"tokenName":"Authorization","tokenValue":"xxx",...},...}}</p>
     */
    private static String extractTokenValue(String json) {
        // 用最朴素的方式定位 "tokenValue":"xxx"，避免引入 JSON 解析依赖
        String marker = "\"tokenValue\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("tokenValue not found in response: " + json);
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            throw new IllegalStateException("tokenValue not terminated in response: " + json);
        }
        return json.substring(valueStart, valueEnd);
    }

    /**
     * 拼接 sa-token 要求的 Authorization header 值。
     * <p>starter 中 {@code SaTokenConfig.setTokenPrefix("Bearer")}，因此读取 header 时会校验前缀。</p>
     */
    private static String authHeader(String tokenValue) {
        return "Bearer " + tokenValue;
    }
}
