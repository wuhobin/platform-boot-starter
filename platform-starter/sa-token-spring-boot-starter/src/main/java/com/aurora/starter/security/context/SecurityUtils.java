package com.aurora.starter.security.context;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aurora.starter.security.account.AccountType;

/**
 * 安全工具类 —— 封装 Sa-Token 常用登录/登出/鉴权操作。
 * <p>
 * 业务代码通过本类操作登录态，无需直接导入 Sa-Token API。
 * </p>
 * <p>
 * <b>双轨制 API：</b>
 * <ul>
 *   <li>无参 / 单参 {@code xxx()} 方法：默认走 {@link AccountType#LOGIN} 账号（仅单账号模式可用）</li>
 *   <li>{@code xxxAs(type, ...)} 方法：多账号体系下显式指定 {@link AccountType}</li>
 * </ul>
 * </p>
 * <p>
 * <b>多账号模式下：</b>所有无参方法入口会校验当前是否为多账号模式（除 login 外
 * 是否有其他 loginType 已注册），若是则抛 {@link IllegalStateException}——
 * 业务方必须使用 {@code xxxAs} 显式指定账号类型。
 * </p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /* ============ 登录 / 登出 ============ */

    /** 默认 {@link AccountType#LOGIN} 账号登录。向后兼容旧代码。 */
    public static void login(Object userId) {
        assertNoMultiAccount();
        StpUtil.login(userId);
    }

    /**
     * 由 {@code SecurityAutoConfiguration} 在启动期设置。
     * true = 业务方声明了除默认 login 以外的账号 → 所有无参 xxx() 方法不可用。
     */
    private static volatile boolean multiAccountMode;

    /**
     * 仅供 {@code SecurityAutoConfiguration} 在启动期调用。
     */
    public static void markMultiAccountMode() {
        multiAccountMode = true;
    }

    /**
     * 多账号模式下禁止使用所有无 AccountType 参数的旧方法——
     * 业务方既已声明多账号，就应显式调用 xxxAs(AccountType, ...) 版本。
     * <p>判定依据为启动期由 {@code SecurityAutoConfiguration} 设置的静态标志位，
     * 而非运行时检查 {@link SaManager#stpLogicMap}（后者是 Sa-Token 懒注册的，
     * 在首次请求到达前可能为空，导致校验滞后）。</p>
     */
    private static void assertNoMultiAccount() {
        if (multiAccountMode) {
            throw new IllegalStateException(
                    "多账号模式下禁止使用无 AccountType 的 xxx() 方法，"
                            + "请改用 xxxAs(AccountType.xxx, ...) 显式指定账号类型");
        }
    }

    /** 指定账号体系登录。 */
    public static void loginAs(AccountType accountType, Object userId) {
        SaManager.getStpLogic(accountType.getCode()).login(userId);
    }

    public static void logout() {
        assertNoMultiAccount();
        StpUtil.logout();
    }

    public static void logoutAs(AccountType accountType) {
        SaManager.getStpLogic(accountType.getCode()).logout();
    }

    /* ============ 状态查询 ============ */

    public static boolean isLogin() {
        assertNoMultiAccount();
        return StpUtil.isLogin();
    }

    public static boolean isLoginAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).isLogin();
    }

    public static Object getLoginId() {
        assertNoMultiAccount();
        return StpUtil.getLoginId();
    }

    public static Object getLoginIdAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).getLoginId();
    }

    /** 获取当前登录用户 ID（Long），指定账号体系。 */
    public static long getLoginIdAsLongAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).getLoginIdAsLong();
    }

    /** 获取当前登录用户 ID（String），指定账号体系。 */
    public static String getLoginIdAsStringAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).getLoginIdAsString();
    }

    public static long getLoginIdAsLong() {
        assertNoMultiAccount();
        return StpUtil.getLoginIdAsLong();
    }

    public static String getLoginIdAsString() {
        assertNoMultiAccount();
        return StpUtil.getLoginIdAsString();
    }

    /* ============ 校验 / 鉴权 ============ */

    public static void checkLogin() {
        assertNoMultiAccount();
        StpUtil.checkLogin();
    }

    public static void checkLoginAs(AccountType accountType) {
        SaManager.getStpLogic(accountType.getCode()).checkLogin();
    }

    public static boolean hasPermission(String permission) {
        assertNoMultiAccount();
        return StpUtil.hasPermission(permission);
    }

    public static boolean hasPermissionAs(AccountType accountType, String permission) {
        return SaManager.getStpLogic(accountType.getCode()).hasPermission(permission);
    }

    public static void checkPermission(String permission) {
        assertNoMultiAccount();
        StpUtil.checkPermission(permission);
    }

    public static void checkPermissionAs(AccountType accountType, String permission) {
        SaManager.getStpLogic(accountType.getCode()).checkPermission(permission);
    }

    public static boolean hasRole(String role) {
        assertNoMultiAccount();
        return StpUtil.hasRole(role);
    }

    public static boolean hasRoleAs(AccountType accountType, String role) {
        return SaManager.getStpLogic(accountType.getCode()).hasRole(role);
    }

    public static void checkRole(String role) {
        assertNoMultiAccount();
        StpUtil.checkRole(role);
    }

    public static void checkRoleAs(AccountType accountType, String role) {
        SaManager.getStpLogic(accountType.getCode()).checkRole(role);
    }

    /* ============ Token / 踢人 ============ */

    public static void kickout(Object userId) {
        assertNoMultiAccount();
        StpUtil.kickout(userId);
    }

    public static void kickoutAs(AccountType accountType, Object userId) {
        SaManager.getStpLogic(accountType.getCode()).kickout(userId);
    }

    public static String getTokenValue() {
        assertNoMultiAccount();
        return StpUtil.getTokenValue();
    }

    public static String getTokenValueAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).getTokenValue();
    }

    public static SaTokenInfo getTokenInfo() {
        assertNoMultiAccount();
        return StpUtil.getTokenInfo();
    }

    public static SaTokenInfo getTokenInfoAs(AccountType accountType) {
        return SaManager.getStpLogic(accountType.getCode()).getTokenInfo();
    }
}
