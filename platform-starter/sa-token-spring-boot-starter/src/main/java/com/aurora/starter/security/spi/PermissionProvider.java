package com.aurora.starter.security.spi;

import com.aurora.starter.security.account.AccountType;

import java.util.Collections;
import java.util.List;

/**
 * 权限/角色提供者接口
 * <p>
 * 业务方实现此接口并注册为 Spring Bean，
 * starter 自动将其适配为 Sa-Token 的 {@link cn.dev33.satoken.stp.StpInterface}。
 * </p>
 * <p>
 * <b>多账号体系下：</b>入参 {@code loginType} 为 {@link AccountType} 枚举值，
 * 业务方可在此参数上 {@code switch} 分派到不同账号的权限/角色数据源
 * （如 {@link AccountType#ADMIN} → 后台权限服务、{@link AccountType#MERCHANT} → 商家权限服务）。
 * </p>
 * <p>
 * <b>向后兼容：</b>未声明多账号时，{@code loginType} 恒为 {@link AccountType#LOGIN}，
 * 行为与旧版本完全一致。
 * </p>
 */
public interface PermissionProvider {

    /**
     * 获取指定用户在指定账号体系下拥有的权限码集合。
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型（如 {@link AccountType#ADMIN} / {@link AccountType#MERCHANT} / {@link AccountType#LOGIN}）
     * @return 权限码列表，不可返回 {@code null}；无权限时返回空列表
     */
    default List<String> getPermissionList(Object loginId, AccountType loginType) {
        return Collections.emptyList();
    }

    /**
     * 获取指定用户在指定账号体系下拥有的角色码集合。
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 角色码列表，不可返回 {@code null}；无角色时返回空列表
     */
    default List<String> getRoleList(Object loginId, AccountType loginType) {
        return Collections.emptyList();
    }
}
