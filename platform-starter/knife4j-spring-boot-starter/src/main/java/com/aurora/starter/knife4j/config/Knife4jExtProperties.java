package com.aurora.starter.knife4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knife4j 扩展配置.
 *
 * <p>仅承载我们关心的 OpenAPI Info 字段（标题/描述/版本/Contact/License）。</p>
 * <p>官方 {@code knife4j.*}（production、basic、setting 等）与 {@code springdoc.*}（group-configs 等）
 * 不在此封装，业务侧直接使用官方原生命名空间，避免重复造轮子。</p>
 *
 * @author whb
 */
@Data
@ConfigurationProperties(prefix = Knife4jExtProperties.PREFIX)
public class Knife4jExtProperties {

    public static final String PREFIX = "platform.knife4j";

    /**
     * 总开关。
     * <p>关闭后不再注入默认 {@link io.swagger.v3.oas.models.OpenAPI} Bean，
     * 但官方 starter 的其它能力（如 /doc.html 静态资源）仍由 knife4j 自身决定。</p>
     */
    private boolean enable = true;

    /** 文档标题. */
    private String title = "Aurora API";

    /** 文档描述. */
    private String description = "";

    /** API 版本. */
    private String version = "1.0.0";

    /** 服务条款 URL，为空则不挂载. */
    private String termsOfServiceUrl;

    /** 联系人信息（任一字段非空时挂载到 OpenAPI.info.contact）. */
    private Contact contact = new Contact();

    /** 协议信息（任一字段非空时挂载到 OpenAPI.info.license）. */
    private License license = new License();

    /**
     * 联系人.
     */
    @Data
    public static class Contact {
        /** 联系人姓名. */
        private String name;
        /** 联系人邮箱. */
        private String email;
        /** 联系人主页 URL. */
        private String url;
    }

    /**
     * 协议.
     */
    @Data
    public static class License {
        /** 协议名称，如 Apache 2.0. */
        private String name;
        /** 协议 URL. */
        private String url;
    }

}
