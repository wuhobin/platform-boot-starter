package com.aurora.starter.webmvc.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内置错误码.
 *
 * @author whb
 */
@Getter
@AllArgsConstructor
public enum DefaultErrorCode implements ErrorCode {

    /** 成功. */
    SUCCESS(0, "成功"),

    /** 参数错误. */
    PARAM_INVALID(400, "参数错误"),

    /** 未认证. */
    UNAUTHORIZED(401, "未认证"),

    /** 无权限. */
    FORBIDDEN(403, "无权限"),

    /** 资源不存在. */
    NOT_FOUND(404, "资源不存在"),

    /** 服务器内部错误. */
    SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
