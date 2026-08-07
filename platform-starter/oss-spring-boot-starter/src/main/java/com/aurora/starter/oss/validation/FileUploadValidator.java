package com.aurora.starter.oss.validation;

import com.aurora.starter.oss.config.FileUploadValidationProperties;
import com.aurora.starter.oss.exception.FileValidationException;
import com.aurora.starter.oss.exception.FileValidationReason;
import org.apache.tika.Tika;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文件上传校验器，包含扩展名白名单校验和 Tika 内容类型检测。
 * <p>
 * 校验器不绑定 Web 业务异常；调用方可根据 {@link FileValidationException} 的原因
 * 转换成自己的错误码。
 */
public class FileUploadValidator {

    private static final Tika TIKA = new Tika();

    private final long maxSizeBytes;
    private final int maxFilenameLength;
    private final Set<String> allowedContentTypes;

    public FileUploadValidator() {
        this(new FileUploadValidationProperties());
    }

    public FileUploadValidator(FileUploadValidationProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("file upload validation properties must not be null");
        }
        DataSize maxSize = properties.getMaxSize();
        if (maxSize == null || maxSize.toBytes() <= 0) {
            throw new IllegalArgumentException("file upload max-size must be positive");
        }
        if (properties.getMaxFilenameLength() <= 0) {
            throw new IllegalArgumentException("file upload max-filename-length must be positive");
        }
        this.maxSizeBytes = maxSize.toBytes();
        this.maxFilenameLength = properties.getMaxFilenameLength();
        this.allowedContentTypes = normalizeAllowedContentTypes(properties.getAllowedContentTypes());
    }

    /**
     * 校验上传文件并返回检测到的 Content-Type。
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw failure(FileValidationReason.EMPTY, "Upload file must not be empty");
        }
        if (file.getSize() > maxSizeBytes) {
            throw failure(FileValidationReason.TOO_LARGE,
                    "Upload file exceeds the configured maximum size");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw failure(FileValidationReason.FILENAME_REQUIRED,
                    "Upload filename must not be blank");
        }
        if (originalFilename.codePointCount(0, originalFilename.length())
                > maxFilenameLength) {
            throw failure(FileValidationReason.FILENAME_TOO_LONG,
                    "Upload filename exceeds the configured maximum length");
        }

        Set<String> expectedContentTypes = expectedContentTypes(originalFilename);
        String detectedContentType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedContentType = normalizeContentType(TIKA.detect(inputStream));
        } catch (IOException exception) {
            throw failure(FileValidationReason.CONTENT_DETECTION_FAILED,
                    "Unable to detect upload content type", exception);
        }
        if (!expectedContentTypes.contains(detectedContentType)) {
            throw new FileValidationException(
                    FileValidationReason.CONTENT_TYPE_MISMATCH,
                    "Upload extension does not match detected content type",
                    expectedContentTypes,
                    detectedContentType);
        }
        return detectedContentType;
    }

    /**
     * 根据原始文件名取得允许的 MIME 类型集合。
     */
    public Set<String> expectedContentTypes(String originalFilename) {
        int lastSeparator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator <= lastSeparator || extensionSeparator == originalFilename.length() - 1) {
            throw failure(FileValidationReason.EXTENSION_NOT_ALLOWED,
                    "Upload filename extension is not allowed");
        }
        String extension = originalFilename.substring(extensionSeparator + 1)
                .toLowerCase(Locale.ROOT);
        String contentType = normalizeContentType(TIKA.detect("file." + extension));
        if (!allowedContentTypes.contains(contentType)) {
            throw failure(FileValidationReason.EXTENSION_NOT_ALLOWED,
                    "Upload filename extension is not allowed");
        }
        return Set.of(contentType);
    }

    private static Set<String> normalizeAllowedContentTypes(List<String> configured) {
        if (configured == null || configured.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String configuredContentType : configured) {
            if (configuredContentType == null || configuredContentType.isBlank()) {
                throw new IllegalArgumentException(
                        "file upload allowed-content-types contains a blank MIME type");
            }
            normalized.add(normalizeContentType(configuredContentType));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        int separator = contentType.indexOf(';');
        String base = separator < 0 ? contentType : contentType.substring(0, separator);
        return base.trim().toLowerCase(Locale.ROOT);
    }

    private static FileValidationException failure(FileValidationReason reason, String message) {
        return new FileValidationException(reason, message);
    }

    private static FileValidationException failure(FileValidationReason reason,
                                                   String message,
                                                   Throwable cause) {
        return new FileValidationException(reason, message, cause);
    }
}
