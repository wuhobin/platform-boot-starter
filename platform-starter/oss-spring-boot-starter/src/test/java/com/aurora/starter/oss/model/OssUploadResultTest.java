package com.aurora.starter.oss.model;

import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OssUploadResultTest {

    @Test
    void keepsTheLegacyFiveArgumentConstructor() {
        OssUploadResult result = new OssUploadResult(
                "https://oss.example.com/file.png", "file.png", 128L, "qiniu-kodo-1", null);

        assertThat(result.getUrl()).isEqualTo("https://oss.example.com/file.png");
        assertThat(result.getFilename()).isEqualTo("file.png");
        assertThat(result.getSize()).isEqualTo(128L);
        assertThat(result.getPlatform()).isEqualTo("qiniu-kodo-1");
    }

    @Test
    void mapsNativeFileInfoMetadata() {
        FileInfo fileInfo = new FileInfo()
                .setId("file-123")
                .setUrl("https://oss.example.com/20260716/report.pdf")
                .setFilename("report.pdf")
                .setOriginalFilename("quarterly-report.pdf")
                .setContentType("application/pdf")
                .setSize(1024L)
                .setPlatform("qiniu-kodo-1")
                .setThUrl("https://oss.example.com/20260716/report-thumb.png");

        OssUploadResult result = OssUploadResult.from(fileInfo);

        assertThat(result.getId()).isEqualTo("file-123");
        assertThat(result.getUrl()).isEqualTo(fileInfo.getUrl());
        assertThat(result.getFilename()).isEqualTo(fileInfo.getFilename());
        assertThat(result.getOriginalFilename()).isEqualTo("quarterly-report.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(1024L);
        assertThat(result.getPlatform()).isEqualTo("qiniu-kodo-1");
        assertThat(result.getThUrl()).isEqualTo(fileInfo.getThUrl());
    }
}
