package com.aurora.starter.webmvc.exception;

/**
 * 业务错误码契约.
 *
 * <p>业务侧可自定义枚举实现本接口扩展错误码集合.</p>
 *
 * @author whb
 */
public interface BizCode {

    /**
     * 业务码，约定与 HTTP 状态码语义对齐（200=成功，4xx=客户端错误，5xx=服务端错误）.
     */
    int getCode();

    /**
     * 默认提示消息.
     */
    String getMessage();
}
