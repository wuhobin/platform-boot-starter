package com.aurora.starter.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.stream.Stream;

/**
 * 文件上传内容校验配置。
 * <p>
 * 配置前缀：{@code platform.oss.upload-validation}。
 * 校验器只有在被业务代码调用时才执行，不提供额外开关。
 */
@Data
@ConfigurationProperties(prefix = "platform.oss.upload-validation")
public class FileUploadValidationProperties {

    public static final long DEFAULT_MAX_SIZE_BYTES = 50L * 1024 * 1024;
    public static final int DEFAULT_MAX_FILENAME_LENGTH = 255;
    private static final List<String> DEFAULT_ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "application/pdf",
            "application/zip",
            "text/plain");

    private DataSize maxSize = DataSize.ofBytes(DEFAULT_MAX_SIZE_BYTES);
    private int maxFilenameLength = DEFAULT_MAX_FILENAME_LENGTH;
    /**
     * 允许的 MIME 类型。默认值覆盖 starter 当前支持的文件格式；业务配置会在此基础上追加，
     * 因此下游只需要声明额外的类型，不必重复配置默认值。
     */
    private List<String> allowedContentTypes = DEFAULT_ALLOWED_CONTENT_TYPES;

    /**
     * Spring Boot 绑定业务配置时追加 MIME 类型，而不是覆盖 starter 默认值。
     */
    public void setAllowedContentTypes(List<String> configuredContentTypes) {
        if (configuredContentTypes == null || configuredContentTypes.isEmpty()) {
            this.allowedContentTypes = DEFAULT_ALLOWED_CONTENT_TYPES;
        } else {
            this.allowedContentTypes = Stream.concat(
                            DEFAULT_ALLOWED_CONTENT_TYPES.stream(), configuredContentTypes.stream())
                    .distinct()
                    .toList();
        }
    }

    /**
     * 暴露 starter 内置的默认 MIME 类型列表。
     */
    public static List<String> getDefaultAllowedContentTypes() {
        return DEFAULT_ALLOWED_CONTENT_TYPES;
    }
}
