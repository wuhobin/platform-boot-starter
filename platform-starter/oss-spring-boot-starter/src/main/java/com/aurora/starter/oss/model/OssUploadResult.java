package com.aurora.starter.oss.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;

/**
 * 文件上传结果 DTO.
 * <p>
 * 从 x-file-storage 的 {@link FileInfo} 中提取关键字段，
 * 使下游工程无需直接依赖 FileInfo 类。
 *
 * @author whb
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssUploadResult {

    /** 文件访问 URL */
    private String url;

    /** 文件名 */
    private String filename;

    /** 文件大小（字节） */
    private Long size;

    /** 存储平台标识 */
    private String platform;

    /** 缩略图 URL（如有） */
    private String thUrl;

    /**
     * 从 FileInfo 创建 OssUploadResult.
     *
     * @param fileInfo x-file-storage 的上传结果
     * @return OssUploadResult
     */
    public static OssUploadResult from(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        return OssUploadResult.builder()
                .url(fileInfo.getUrl())
                .filename(fileInfo.getFilename())
                .size(fileInfo.getSize())
                .platform(fileInfo.getPlatform())
                .thUrl(fileInfo.getThUrl())
                .build();
    }
}
