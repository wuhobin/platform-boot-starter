package com.aurora.starter.oss.config;

import com.aurora.starter.oss.validation.FileUploadValidator;
import org.apache.tika.Tika;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartFile;

/** 文件上传内容校验自动配置。 */
@AutoConfiguration
@ConditionalOnClass({Tika.class, MultipartFile.class})
@EnableConfigurationProperties(FileUploadValidationProperties.class)
public class FileUploadValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FileUploadValidator fileUploadValidator(
            FileUploadValidationProperties properties) {
        return new FileUploadValidator(properties);
    }
}
