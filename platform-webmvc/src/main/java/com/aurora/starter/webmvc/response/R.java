package com.aurora.starter.webmvc.response;

import com.aurora.starter.webmvc.exception.DefaultErrorCode;
import com.aurora.starter.webmvc.exception.ErrorCode;
import com.aurora.starter.webmvc.trace.TraceContext;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体.
 *
 * <p>HTTP 状态码统一返回 200，业务结果由 {@link #code} 字段承载。</p>
 *
 * @param <T> data 类型
 * @author whb
 */
@Data
@NoArgsConstructor
public class R<T> {

    /** 业务码，0 表示成功. */
    private int code;

    /** 提示消息. */
    private String message;

    /** 业务数据. */
    private T data;

    /** 服务器时间戳（毫秒）. */
    private long timestamp;

    /** 追踪 ID，构造时从 {@link TraceContext} 取，未配置 trace 时为 null. */
    private String traceId;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = DefaultErrorCode.SUCCESS.getCode();
        r.message = DefaultErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        r.traceId = TraceContext.get();
        return r;
    }

    public static <T> R<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        r.traceId = TraceContext.get();
        return r;
    }
}
