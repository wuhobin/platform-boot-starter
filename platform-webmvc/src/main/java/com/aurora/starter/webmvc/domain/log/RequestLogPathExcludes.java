package com.aurora.starter.webmvc.domain.log;

import java.util.List;

/**
 * 请求日志排除路径与后缀.
 *
 * @author whb
 */
public final class RequestLogPathExcludes {

    /** Ant 风格路径模式，命中则跳过日志. */
    public static final List<String> PATH_PATTERNS = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico"
    );

    /** 文件后缀（小写），命中则跳过日志. */
    public static final List<String> SUFFIXES = List.of(
            ".js", ".css", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".woff", ".woff2"
    );

    private RequestLogPathExcludes() {
    }
}
