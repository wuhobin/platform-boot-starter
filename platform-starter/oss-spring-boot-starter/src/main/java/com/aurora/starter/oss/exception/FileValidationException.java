package com.aurora.starter.oss.exception;

import lombok.Getter;

import java.util.Set;

/**
 * 文件上传内容校验异常。
 * <p>
 * 下游可以根据 {@link #getReason()} 映射为自身的 API 错误码或业务文案，
 * starter 不绑定具体的 Web 异常体系。
 */
@Getter
public class FileValidationException extends RuntimeException {

    private final FileValidationReason reason;
    private final Set<String> expectedContentTypes;
    private final String detectedContentType;

    public FileValidationException(FileValidationReason reason, String message) {
        this(reason, message, null, Set.of(), null);
    }

    public FileValidationException(FileValidationReason reason, String message, Throwable cause) {
        this(reason, message, cause, Set.of(), null);
    }

    public FileValidationException(FileValidationReason reason,
                                   String message,
                                   Set<String> expectedContentTypes,
                                   String detectedContentType) {
        this(reason, message, null, expectedContentTypes, detectedContentType);
    }

    public FileValidationException(FileValidationReason reason,
                                   String message,
                                   Throwable cause,
                                   Set<String> expectedContentTypes,
                                   String detectedContentType) {
        super(message, cause);
        this.reason = reason;
        this.expectedContentTypes = expectedContentTypes == null
                ? Set.of() : Set.copyOf(expectedContentTypes);
        this.detectedContentType = detectedContentType;
    }
}
