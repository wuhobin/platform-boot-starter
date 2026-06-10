package com.aurora.starter.webmvc.trace;

import com.aurora.starter.webmvc.constants.TraceConstants;
import com.aurora.starter.webmvc.domain.response.Result;
import org.slf4j.MDC;

/**
 * TraceId 上下文：封装 SLF4J MDC + ThreadLocal 双通道存取.
 *
 * <p>MDC 通道供日志框架自动输出 traceId；ThreadLocal 通道供业务代码同步读取
 * （如 {@link Result} 构造时回填）。</p>
 *
 * @author whb
 */
public final class TraceContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    /**
     * 写入 traceId 到 MDC 和 ThreadLocal.
     */
    public static void set(String traceId) {
        HOLDER.set(traceId);
        MDC.put(TraceConstants.MDC_KEY, traceId);
    }

    /**
     * 读取当前线程的 traceId，可能为 null.
     */
    public static String get() {
        return HOLDER.get();
    }

    /**
     * 清理 MDC 和 ThreadLocal，必须在请求结束 finally 块调用，防止 ThreadLocal 泄漏.
     */
    public static void clear() {
        HOLDER.remove();
        MDC.remove(TraceConstants.MDC_KEY);
    }
}