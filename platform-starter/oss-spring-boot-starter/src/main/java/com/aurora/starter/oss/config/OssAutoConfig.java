package com.aurora.starter.oss.config;

import com.aurora.starter.oss.template.OssTemplate;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * OSS 文件存储自动配置.
 * <p>
 * 仅在 classpath 中存在 x-file-storage 的 {@link FileStorageService} 时激活。
 *
 * @author whb
 */
@AutoConfiguration
@ConditionalOnClass(FileStorageService.class)
public class OssAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(FileStorageService fileStorageService) {
        return new OssTemplate(fileStorageService);
    }
}
