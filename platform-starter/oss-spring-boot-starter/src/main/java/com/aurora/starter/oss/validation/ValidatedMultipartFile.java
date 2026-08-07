package com.aurora.starter.oss.validation;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * MultipartFile 装饰器，将 Content-Type 替换为服务端检测到的真实类型。
 */
public class ValidatedMultipartFile implements MultipartFile {

    private final MultipartFile delegate;
    private final String contentType;

    public ValidatedMultipartFile(MultipartFile delegate, String contentType) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getOriginalFilename() {
        return delegate.getOriginalFilename();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return delegate.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        delegate.transferTo(dest);
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        delegate.transferTo(dest);
    }
}
