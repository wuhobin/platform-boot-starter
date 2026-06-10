package com.aurora.starter.webmvc.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 透传 Filter.
 *
 * <p>请求进入：从 header {@link TraceConstants#HEADER_NAME} 取，缺失则生成 32 位 UUID（去掉横线）。
 * 写入 {@link TraceContext}，响应 header 回填。请求结束必须清理 ThreadLocal。</p>
 *
 * <p>继承 {@link OncePerRequestFilter} 避免 forward/include 场景下被重复执行造成的 MDC 清理早于外层 finally 的问题。</p>
 *
 * <p>顺序 {@link Ordered#HIGHEST_PRECEDENCE} + 1，确保比 {@code RequestLogFilter} 先执行，
 * 使后续日志均能输出 traceId。</p>
 *
 * @author whb
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TraceConstants.HEADER_NAME);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            TraceContext.set(traceId);
            response.setHeader(TraceConstants.HEADER_NAME, traceId);
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
