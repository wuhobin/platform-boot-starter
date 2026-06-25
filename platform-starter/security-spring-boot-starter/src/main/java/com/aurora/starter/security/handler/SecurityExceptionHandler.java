package com.aurora.starter.security.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.webmvc.enums.DefaultBizCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 安全异常统一处理
 * <p>
 * 将 Sa-Token 的各类认证/鉴权异常转换为项目统一的 Result 响应体。
 * </p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.debug("未登录: {}", e.getMessage());
        return Result.error(DefaultBizCode.UNAUTHORIZED, "未登录");
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermission(NotPermissionException e) {
        log.debug("权限不足: {}", e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "无权限");
    }

    /**
     * 角色不匹配异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRole(NotRoleException e) {
        log.debug("角色不匹配: {}", e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "无权限");
    }

    /**
     * 账号封禁异常
     */
    @ExceptionHandler(DisableServiceException.class)
    public Result<Void> handleDisableService(DisableServiceException e) {
        log.debug("账号被封禁: {}", e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "账号已被封禁");
    }

    /**
     * 其他 Sa-Token 异常
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> handleSaTokenException(SaTokenException e) {
        log.warn("Sa-Token 异常: {}", e.getMessage());
        return Result.error(DefaultBizCode.SERVER_ERROR, e.getMessage());
    }
}
