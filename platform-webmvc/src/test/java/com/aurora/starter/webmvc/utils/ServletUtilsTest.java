package com.aurora.starter.webmvc.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class ServletUtilsTest {

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsCurrentServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(ServletUtils.getRequest()).isSameAs(request);
    }

    @Test
    void returnsEmptyValuesOutsideServletRequest() {
        assertThat(ServletUtils.getRequest()).isNull();
        assertThat(ServletUtils.getHeader("User-Agent")).isNull();
        assertThat(ServletUtils.getClientIp()).isEmpty();
    }

    @Test
    void usesFirstForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("192.0.2.30");

        assertThat(ServletUtils.getClientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void ignoresUnknownForwardedIpAndUsesRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "198.51.100.20");

        assertThat(ServletUtils.getClientIp(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void fallsBackToRemoteAddressAndNormalizesLoopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        assertThat(ServletUtils.getClientIp(request)).isEqualTo("127.0.0.1");

        request.setRemoteAddr("::1");
        assertThat(ServletUtils.getClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void readsHeaderFromExplicitRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "test-agent");

        assertThat(ServletUtils.getHeader(request, "User-Agent")).isEqualTo("test-agent");
    }
}
