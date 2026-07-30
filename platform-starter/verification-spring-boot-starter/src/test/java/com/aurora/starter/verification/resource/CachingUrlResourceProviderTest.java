package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.resource.common.model.dto.Resource;
import com.aurora.starter.verification.exception.ImageVerificationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingUrlResourceProviderTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsOnceAndReturnsIndependentCachedStreams() throws IOException {
        byte[] image = "fake-png-content".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/captcha.png", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, "image/png", image);
        });
        CachingUrlResourceProvider provider =
                new CachingUrlResourceProvider(1_000, 1_000, 1_024, 8);
        Resource resource = new Resource("url", baseUrl + "/captcha.png", "default");

        byte[] first;
        byte[] second;
        try (var input = provider.getResourceInputStream(resource)) {
            first = input.readAllBytes();
        }
        try (var input = provider.getResourceInputStream(resource)) {
            second = input.readAllBytes();
        }

        assertThat(first).isEqualTo(image);
        assertThat(second).isEqualTo(image);
        assertThat(requests).hasValue(1);
    }

    @Test
    void rejectsOversizedRemoteContent() {
        byte[] image = new byte[32];
        server.createContext("/large.png", exchange ->
                respond(exchange, 200, "image/png", image));
        CachingUrlResourceProvider provider =
                new CachingUrlResourceProvider(1_000, 1_000, 16, 8);

        assertThatThrownBy(() -> provider.getResourceInputStream(
                new Resource("url", baseUrl + "/large.png")))
                .isInstanceOf(ImageVerificationException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void rejectsEmptyRemoteContent() {
        server.createContext("/empty.png", exchange ->
                respond(exchange, 200, "image/png", new byte[0]));
        CachingUrlResourceProvider provider =
                new CachingUrlResourceProvider(1_000, 1_000, 1_024, 8);

        assertThatThrownBy(() -> provider.getResourceInputStream(
                new Resource("url", baseUrl + "/empty.png")))
                .isInstanceOf(ImageVerificationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsNonImageResponsesAndUnsupportedSchemes() {
        server.createContext("/text", exchange ->
                respond(exchange, 200, "text/plain", new byte[]{1, 2, 3}));
        CachingUrlResourceProvider provider =
                new CachingUrlResourceProvider(1_000, 1_000, 1_024, 8);

        assertThatThrownBy(() -> provider.getResourceInputStream(
                new Resource("url", baseUrl + "/text")))
                .isInstanceOf(ImageVerificationException.class)
                .hasMessageContaining("not an image");
        assertThatThrownBy(() -> provider.getResourceInputStream(
                new Resource("url", "file:///tmp/captcha.png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP(S)");
    }

    @Test
    void identifiesUrlResourcesCaseInsensitively() {
        CachingUrlResourceProvider provider = new CachingUrlResourceProvider();

        assertThat(provider.getName()).isEqualTo("URL");
        assertThat(provider.supported(new Resource("url", "https://example.com/a.png")))
                .isTrue();
        assertThat(provider.supported(new Resource("URL", "https://example.com/a.png")))
                .isTrue();
        assertThat(provider.supported(new Resource("file", "/a.png"))).isFalse();
        assertThat(provider.supported(null)).isFalse();
    }

    private void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
