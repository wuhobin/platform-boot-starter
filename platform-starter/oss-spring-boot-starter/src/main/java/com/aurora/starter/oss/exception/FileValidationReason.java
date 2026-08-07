package com.aurora.starter.oss.exception;

/** 文件上传校验失败原因。 */
public enum FileValidationReason {
    EMPTY,
    TOO_LARGE,
    FILENAME_REQUIRED,
    FILENAME_TOO_LONG,
    EXTENSION_NOT_ALLOWED,
    CONTENT_DETECTION_FAILED,
    CONTENT_TYPE_MISMATCH
}
