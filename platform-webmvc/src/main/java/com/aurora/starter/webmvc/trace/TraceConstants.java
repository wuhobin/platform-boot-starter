package com.aurora.starter.webmvc.trace;

/**
 * Trace 相关常量.
 *
 * @author whb
 */
public final class TraceConstants {

    /** HTTP header 名，请求/响应均使用. */
    public static final String HEADER_NAME = "X-Trace-Id";

    /** SLF4J MDC 中存放 traceId 的 key. */
    public static final String MDC_KEY = "traceId";

    private TraceConstants() {
    }
}
