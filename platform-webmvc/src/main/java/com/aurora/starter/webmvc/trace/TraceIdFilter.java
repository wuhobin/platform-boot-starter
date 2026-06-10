package com.aurora.starter.webmvc.trace;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 透传 Filter.
 *
 * <p>请求进入：从 header {@link TraceConstants#HEADER_NAME} 取，缺失则生成 32 位 UUID（去掉横线）。
 * 写入 {@link TraceContext}，响应 header 回填。请求结束必须清理 ThreadLocal。</p>
 *
 * <p>顺序 {@link Ordered#HIGHEST_PRECEDENCE} + 1，确保比 {@code RequestLogFilter} 先执行，
 * 使后续日志均能输出 traceId。</p>
 *
 * @author whb
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(TraceConstants.HEADER_NAME);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            TraceContext.set(traceId);
            httpResponse.setHeader(TraceConstants.HEADER_NAME, traceId);
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
