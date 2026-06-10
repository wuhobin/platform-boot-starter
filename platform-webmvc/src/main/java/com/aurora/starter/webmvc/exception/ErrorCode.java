package com.aurora.starter.webmvc.exception;

/**
 * 错误码契约.
 *
 * <p>业务侧可自定义枚举实现本接口扩展错误码集合.</p>
 *
 * @author whb
 */
public interface ErrorCode {

    /**
     * 业务码，0 约定为成功，其它为各类错误.
     */
    int getCode();

    /**
     * 默认提示消息.
     */
    String getMessage();
}
