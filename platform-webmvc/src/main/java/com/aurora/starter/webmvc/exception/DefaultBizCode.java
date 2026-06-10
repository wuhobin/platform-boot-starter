package com.aurora.starter.webmvc.exception;

import com.aurora.starter.webmvc.constants.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内置业务错误码.
 *
 * @author whb
 */
@Getter
@AllArgsConstructor
public enum DefaultBizCode implements BizCode {

    /** 成功. */
    SUCCESS(HttpStatus.SUCCESS, "成功"),

    /** 参数错误. */
    PARAM_INVALID(HttpStatus.PARAM_INVALID, "参数错误"),

    /** 未认证. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未认证"),

    /** 无权限. */
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权限"),

    /** 资源不存在. */
    NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),

    /** 服务器内部错误. */
    SERVER_ERROR(HttpStatus.ERROR, "服务器内部错误");

    private final int code;
    private final String message;
}
