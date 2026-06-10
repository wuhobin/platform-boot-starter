package com.aurora.starter.webmvc.exception;

import lombok.Getter;

/**
 * 业务异常.
 *
 * <p>{@link RuntimeException} 子类，不污染业务方法签名。
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为 {@link com.aurora.starter.webmvc.response.R}.</p>
 *
 * @author whb
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务码. */
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
