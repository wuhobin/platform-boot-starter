package com.aurora.starter.webmvc.log;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 请求日志 Filter.
 *
 * <p>输出格式：{@code method={} uri={} status={} duration={}ms ip={}}。
 * traceId 由 MDC 自动带出（依赖 logback/log4j2 的 pattern 已配置 %X{traceId}）。</p>
 *
 * <p>顺序 {@link Ordered#HIGHEST_PRECEDENCE} + 100，晚于 {@link com.aurora.starter.webmvc.trace.TraceIdFilter}。</p>
 *
 * @author whb
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RequestLogFilter implements Filter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();

        if (skip(uri)) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("method={} uri={} status={} duration={}ms ip={}",
                    httpRequest.getMethod(), uri, httpResponse.getStatus(),
                    duration, clientIp(httpRequest));
        }
    }

    private static boolean skip(String uri) {
        for (String pattern : RequestLogPathExcludes.PATH_PATTERNS) {
            if (MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        String lower = uri.toLowerCase();
        for (String suffix : RequestLogPathExcludes.SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * IP 取值顺序：X-Forwarded-For 首段 → X-Real-IP → request.getRemoteAddr().
     */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xri)) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
