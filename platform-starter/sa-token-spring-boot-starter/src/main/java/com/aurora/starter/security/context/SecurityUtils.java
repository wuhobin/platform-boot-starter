package com.aurora.starter.security.context;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 安全工具类 —— 封装 Sa-Token StpUtil 常用操作
 * <p>
 * 业务代码通过本类操作登录/登出/鉴权，无需直接导入 Sa-Token API。
 * 默认使用 Sa-Token 的 "login" 登录类型。
 * </p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 登录，为当前会话生成 Token
     *
     * @param userId 用户 ID
     */
    public static void login(Object userId) {
        StpUtil.login(userId);
    }

    /**
     * 登出当前会话
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * 判断当前会话是否已登录
     *
     * @return true 已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID
     */
    public static Object getLoginId() {
        return StpUtil.getLoginId();
    }

    /**
     * 获取当前登录用户 ID (Long)
     *
     * @return 用户 ID
     */
    public static long getLoginIdAsLong() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户 ID (String)
     *
     * @return 用户 ID
     */
    public static String getLoginIdAsString() {
        return StpUtil.getLoginIdAsString();
    }

    /**
     * 判断当前会话是否拥有指定权限
     *
     * @param permission 权限码
     * @return true 拥有该权限
     */
    public static boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    /**
     * 判断当前会话是否拥有指定角色
     *
     * @param role 角色码
     * @return true 拥有该角色
     */
    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * 校验权限，不通过则抛出 NotPermissionException
     *
     * @param permission 权限码
     */
    public static void checkPermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    /**
     * 校验角色，不通过则抛出 NotRoleException
     *
     * @param role 角色码
     */
    public static void checkRole(String role) {
        StpUtil.checkRole(role);
    }

    /**
     * 踢人下线
     *
     * @param userId 用户 ID
     */
    public static void kickout(Object userId) {
        StpUtil.kickout(userId);
    }

    /**
     * 获取当前 Token 值
     *
     * @return Token 字符串
     */
    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 获取当前 Token 信息
     *
     * @return Token 信息对象
     */
    public static cn.dev33.satoken.stp.SaTokenInfo getTokenInfo() {
        return StpUtil.getTokenInfo();
    }
}
