package com.aurora.starter.security.account;

import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.security.spi.PermissionProvider;

/**
 * 内置账号体系标识，对应 Sa-Token 的 {@code loginType}。
 * <p>
 * 所有 {@link SecurityUtils} 多账号方法、{@link AccountTypeDefinition}、{@link PermissionProvider}
 * 均使用本枚举代替魔法字符串，编译期拦截拼写错误。
 * </p>
 * <p>
 * <b>Sa-Token 注解例外：</b>{@code @SaCheckLogin(type = "admin")} 的 {@code type}
 * 属性仍为 {@code String}——Java 注解不支持枚举方法调用作为属性值。
 * </p>
 * <p>
 * <b>自定义 loginType：</b>本枚举为封闭定义。业务方若需新类型应先在此处添加枚举值，
 * 然后用 {@code AccountType.NEW_TYPE} 写入 {@link AccountTypeDefinition} Bean。
 * </p>
 */
public enum AccountType {

    /** 默认账号，对应 Sa-Token 内置 {@code "login"} 类型 */
    LOGIN("login", "默认账号"),

    /** C 端用户 */
    USER("user", "C端用户"),

    /** 后台管理员 */
    ADMIN("admin", "后台管理员"),

    /** 商家 */
    MERCHANT("merchant", "商家");

    private final String code;
    private final String description;

    AccountType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 返回对应 Sa-Token {@code loginType} 的字符串值，
     * 用于传递给 {@code SaManager.getStpLogic(code)} 等底层 API。
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回中文描述，供启动日志等可读性场景使用。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据 Sa-Token {@code loginType} 字符串查找对应枚举。
     * <p>找不到时返回 {@code null}——调用方需降级处理。</p>
     */
    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
