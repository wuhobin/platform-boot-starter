package com.aurora.starter.webmvc.exception;

import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.webmvc.enums.DefaultBizCode;
import lombok.Getter;

/**
 * 业务异常.
 *
 * <p>{@link RuntimeException} 子类，不污染业务方法签名。
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为 {@link Result}.</p>
 *
 * @author whb
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务码. */
    private final int code;

    public BizException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }

    public BizException(BizCode bizCode, String customMessage) {
        super(customMessage);
        this.code = bizCode.getCode();
    }

    public BizException(String message) {
        this(DefaultBizCode.SERVER_ERROR, message);
    }

    public BizException(String message, Throwable cause) {
        this(DefaultBizCode.SERVER_ERROR.getCode(), message, cause);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(Throwable cause) {
        this(DefaultBizCode.SERVER_ERROR.getCode(), cause.getMessage(), cause);
    }
}
