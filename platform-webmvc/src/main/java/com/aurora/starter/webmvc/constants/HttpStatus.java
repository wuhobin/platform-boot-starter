package com.aurora.starter.webmvc.constants;

/**
 * HTTP 风格的业务状态码常量.
 *
 * <p>本模块业务码采用 HTTP 状态码语义：200 成功、4xx 客户端错误、5xx 服务端错误。</p>
 *
 * @author whb
 */
public final class HttpStatus {

    /** 操作成功. */
    public static final int SUCCESS = 200;

    /** 参数错误. */
    public static final int PARAM_INVALID = 400;

    /** 未认证. */
    public static final int UNAUTHORIZED = 401;

    /** 无权限. */
    public static final int FORBIDDEN = 403;

    /** 资源不存在. */
    public static final int NOT_FOUND = 404;

    /** 服务器内部错误. */
    public static final int ERROR = 500;

    private HttpStatus() {
    }
}
