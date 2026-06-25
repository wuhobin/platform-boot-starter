package com.aurora.starter.security.spi;

import java.util.Collections;
import java.util.List;

/**
 * 权限/角色提供者接口
 * <p>
 * 业务方实现此接口并注册为 Spring Bean，
 * starter 自动将其适配为 Sa-Token 的 StpInterface。
 * </p>
 */
public interface PermissionProvider {

    /**
     * 获取指定用户拥有的权限码集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型（对应 Sa-Token loginType）
     * @return 权限码列表，不可返回 null
     */
    default List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 获取指定用户拥有的角色码集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型（对应 Sa-Token loginType）
     * @return 角色码列表，不可返回 null
     */
    default List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
