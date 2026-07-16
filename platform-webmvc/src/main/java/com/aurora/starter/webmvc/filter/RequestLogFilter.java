package com.aurora.starter.webmvc.filter;

import com.aurora.starter.webmvc.domain.log.RequestLogPathExcludes;
import com.aurora.starter.webmvc.utils.ServletUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs one summary line for each non-static HTTP request.
 * Trace IDs are rendered by the logging pattern from MDC.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
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
                    request.getMethod(), uri, response.getStatus(),
                    duration, ServletUtils.getClientIp(request));
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
}
