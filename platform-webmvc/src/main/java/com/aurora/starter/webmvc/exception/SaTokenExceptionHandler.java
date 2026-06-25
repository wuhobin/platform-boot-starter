package com.aurora.starter.webmvc.exception;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.webmvc.enums.DefaultBizCode;
import jakarta.servlet.http.HttpServletRequest;
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
 * 当 classpath 中存在 Sa-Token 时自动生效。
 * </p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SaTokenExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SaTokenExceptionHandler.class);

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e, HttpServletRequest request) {
        log.debug("[{} {}] 未登录: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(DefaultBizCode.UNAUTHORIZED, "未登录");
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermission(NotPermissionException e, HttpServletRequest request) {
        log.debug("[{} {}] 权限不足: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "无权限");
    }

    /**
     * 角色不匹配异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRole(NotRoleException e, HttpServletRequest request) {
        log.debug("[{} {}] 角色不匹配: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "无权限");
    }

    /**
     * 账号封禁异常
     */
    @ExceptionHandler(DisableServiceException.class)
    public Result<Void> handleDisableService(DisableServiceException e, HttpServletRequest request) {
        log.debug("[{} {}] 账号被封禁: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(DefaultBizCode.FORBIDDEN, "账号已被封禁");
    }

    /**
     * 其他 Sa-Token 异常
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> handleSaTokenException(SaTokenException e, HttpServletRequest request) {
        log.warn("[{} {}] Sa-Token 异常: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(DefaultBizCode.SERVER_ERROR, e.getMessage());
    }
}
