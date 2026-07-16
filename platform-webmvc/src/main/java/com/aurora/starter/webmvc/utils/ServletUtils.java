package com.aurora.starter.webmvc.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet request helpers shared by WebMVC infrastructure and applications.
 */
public final class ServletUtils {

    private static final String UNKNOWN = "unknown";
    private static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String IPV6_LOOPBACK = "::1";
    private static final String IPV6_LOOPBACK_EXPANDED = "0:0:0:0:0:0:0:1";

    private ServletUtils() {
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    public static String getHeader(String name) {
        return getHeader(getRequest(), name);
    }

    public static String getHeader(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    public static String getClientIp() {
        return getClientIp(getRequest());
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String ip = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (!isUsableIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!isUsableIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (IPV6_LOOPBACK.equals(ip) || IPV6_LOOPBACK_EXPANDED.equals(ip)) {
            return IPV4_LOOPBACK;
        }
        return StringUtils.hasText(ip) ? ip.trim() : "";
    }

    private static String firstForwardedIp(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return forwardedFor;
        }
        int comma = forwardedFor.indexOf(',');
        return (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
    }

    private static boolean isUsableIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip.trim());
    }
}
