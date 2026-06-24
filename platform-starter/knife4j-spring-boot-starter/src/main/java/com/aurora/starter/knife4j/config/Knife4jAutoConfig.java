package com.aurora.starter.knife4j.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Knife4j 自动装配.
 *
 * <p>提供一个默认 {@link OpenAPI} Bean（标题/描述/版本/Contact/License 来自 {@link Knife4jExtProperties}），
 * 标注 {@link ConditionalOnMissingBean}，业务工程可整体覆盖以追加 SecurityScheme、Server 列表等。</p>
 *
 * @author whb
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = Knife4jExtProperties.PREFIX, name = "enable",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(Knife4jExtProperties.class)
public class Knife4jAutoConfig {

    /**
     * 默认 OpenAPI Bean.
     *
     * @param props 扩展配置
     * @return 已挂载 Info 的 OpenAPI 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI auroraOpenApi(Knife4jExtProperties props) {
        Info info = new Info()
                .title(props.getTitle())
                .description(props.getDescription())
                .version(props.getVersion());

        if (StringUtils.hasText(props.getTermsOfServiceUrl())) {
            info.termsOfService(props.getTermsOfServiceUrl());
        }
        if (hasAny(props.getContact())) {
            info.contact(toSwaggerContact(props.getContact()));
        }
        if (hasAny(props.getLicense())) {
            info.license(toSwaggerLicense(props.getLicense()));
        }
        return new OpenAPI().info(info);
    }

    private static boolean hasAny(Knife4jExtProperties.Contact contact) {
        return contact != null && (StringUtils.hasText(contact.getName())
                || StringUtils.hasText(contact.getEmail())
                || StringUtils.hasText(contact.getUrl()));
    }

    private static boolean hasAny(Knife4jExtProperties.License license) {
        return license != null && (StringUtils.hasText(license.getName())
                || StringUtils.hasText(license.getUrl()));
    }

    private static Contact toSwaggerContact(Knife4jExtProperties.Contact contact) {
        Contact target = new Contact();
        if (StringUtils.hasText(contact.getName())) {
            target.name(contact.getName());
        }
        if (StringUtils.hasText(contact.getEmail())) {
            target.email(contact.getEmail());
        }
        if (StringUtils.hasText(contact.getUrl())) {
            target.url(contact.getUrl());
        }
        return target;
    }

    private static License toSwaggerLicense(Knife4jExtProperties.License license) {
        License target = new License();
        if (StringUtils.hasText(license.getName())) {
            target.name(license.getName());
        }
        if (StringUtils.hasText(license.getUrl())) {
            target.url(license.getUrl());
        }
        return target;
    }

}
