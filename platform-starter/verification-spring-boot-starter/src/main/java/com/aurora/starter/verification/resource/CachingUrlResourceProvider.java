package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.resource.ResourceProvider;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import com.aurora.starter.verification.exception.ImageVerificationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 带超时、大小限制和进程内缓存的 tianai URL 资源提供器。
 */
public class CachingUrlResourceProvider implements ResourceProvider {

    public static final String NAME = "URL";

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5_000;
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
    private static final int DEFAULT_MAX_CACHE_ENTRIES = 32;

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int maxContentLength;
    private final Map<String, byte[]> cache;

    public CachingUrlResourceProvider() {
        this(
                DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_READ_TIMEOUT_MILLIS,
                DEFAULT_MAX_CONTENT_LENGTH,
                DEFAULT_MAX_CACHE_ENTRIES);
    }

    public CachingUrlResourceProvider(
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int maxContentLength,
            int maxCacheEntries) {
        if (connectTimeoutMillis <= 0
                || readTimeoutMillis <= 0
                || maxContentLength <= 0
                || maxCacheEntries <= 0) {
            throw new IllegalArgumentException("URL resource limits must be positive");
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.maxContentLength = maxContentLength;
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                        return size() > maxCacheEntries;
                    }
                });
    }

    @Override
    public InputStream getResourceInputStream(Resource resource) {
        if (resource == null || resource.getData() == null || resource.getData().isBlank()) {
            throw new IllegalArgumentException("URL resource must not be blank");
        }
        String location = resource.getData().trim();
        synchronized (cache) {
            byte[] content = cache.get(location);
            if (content == null) {
                content = download(location);
                cache.put(location, content);
            }
            return new ByteArrayInputStream(content);
        }
    }

    @Override
    public boolean supported(Resource resource) {
        return resource != null && NAME.equalsIgnoreCase(resource.getType());
    }

    @Override
    public String getName() {
        return NAME;
    }

    private byte[] download(String location) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(location);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Only HTTP(S) image resources are supported");
            }

            URLConnection urlConnection = uri.toURL().openConnection();
            urlConnection.setConnectTimeout(connectTimeoutMillis);
            urlConnection.setReadTimeout(readTimeoutMillis);
            urlConnection.setUseCaches(false);
            if (!(urlConnection instanceof HttpURLConnection httpConnection)) {
                throw new IllegalArgumentException("Only HTTP(S) image resources are supported");
            }
            connection = httpConnection;
            connection.setInstanceFollowRedirects(true);

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new ImageVerificationException(
                        "Remote image resource returned HTTP " + status);
            }
            long declaredLength = connection.getContentLengthLong();
            if (declaredLength > maxContentLength) {
                throw new ImageVerificationException("Remote image resource is too large");
            }
            String contentType = connection.getContentType();
            if (contentType != null
                    && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new ImageVerificationException("Remote resource is not an image");
            }

            try (InputStream input = connection.getInputStream()) {
                byte[] content = readBounded(input);
                if (content.length == 0) {
                    throw new ImageVerificationException("Remote image resource is empty");
                }
                return content;
            }
        } catch (IOException ex) {
            throw new ImageVerificationException("Failed to load remote image resource", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxContentLength) {
                throw new ImageVerificationException("Remote image resource is too large");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
