package com.aurora.starter.security.account;

import java.util.List;

/**
 * 业务方声明"账号体系"的 SPI。
 * <p>
 * 每个 {@code @Bean} 声明一个独立的身份空间（如 admin / merchant / c端用户），
 * starter 收集所有 Bean 并据此生成 SaRouter 路由规则 + 提供 SecurityUtils.*As 入口。
 * </p>
 * <p>
 * <b>账号隔离：</b>每种账号在 Sa-Token 内部有独立 token 命名空间、独立会话、独立持久化 key，
 * 不同账号的登录态互不污染。
 * </p>
 */
public interface AccountTypeDefinition {

    /**
     * 账号体系标识，对应 Sa-Token {@code loginType}。
     * <p>必须全局唯一；冲突时启动失败（见 {@link AccountTypeRegistry}）。</p>
     */
    AccountType getType();

    /**
     * 该账号需要登录才能访问的 URL Ant 模式列表（如 {@code ["/admin/**"]}）。
     * <p>
     * <b>空列表 = 仅注解级生效</b>：该账号不参与 SaRouter 路由，
     * 仍可在方法级通过 {@code @SaCheckLogin(type="...")} 注解生效。
     * </p>
     */
    default List<String> getPaths() {
        return List.of();
    }

    /**
     * 可选描述，仅用于启动日志输出。
     */
    default String getDescription() {
        return "";
    }
}