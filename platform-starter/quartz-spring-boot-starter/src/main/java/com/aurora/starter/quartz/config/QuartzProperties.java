package com.aurora.starter.quartz.config;

import com.aurora.starter.quartz.enums.MisfirePolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台 Quartz 配置属性 —— 前缀 {@code platform.quartz}.
 */
@Data
@ConfigurationProperties(prefix = "platform.quartz")
public class QuartzProperties {

    /** 总开关,默认 true. */
    private boolean enabled = true;

    /** 创建任务时未指定 group 的默认值. */
    private String defaultJobGroup = "DEFAULT";

    /** 默认 misfire 策略. */
    private MisfirePolicy defaultMisfirePolicy = MisfirePolicy.DEFAULT;

    /** 日志相关配置. */
    private Log log = new Log();

    /** 是否自动建表,默认 false.设置为 true 时启动会执行 CREATE TABLE IF NOT EXISTS. */
    private boolean autoInitTable = false;

    @Data
    public static class Log {
        /** 异常信息最大保留字符,默认 2000. */
        private int maxExceptionLength = 2000;

        /** invokeTarget 关键字列表,命中的任务不生成日志. */
        private List<String> skipList = new ArrayList<>();
    }
}
