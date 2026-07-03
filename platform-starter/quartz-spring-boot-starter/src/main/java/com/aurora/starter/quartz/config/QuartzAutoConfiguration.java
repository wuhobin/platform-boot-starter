package com.aurora.starter.quartz.config;

import com.aurora.starter.quartz.bootstrap.JobBootstrap;
import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.handler.impl.DbJobLogHandler;
import com.aurora.starter.quartz.core.schedule.DefaultScheduleManager;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import com.aurora.starter.quartz.mapper.QuartzJobMapper;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.aurora.starter.quartz.service.impl.QuartzJobServiceImpl;
import com.aurora.starter.quartz.util.JobInvokeUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.quartz.Scheduler;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Quartz Starter 自动配置.
 * <p>
 * 所有组件通过 @Bean 注册，业务方无需手动 scan / MapperScan 即可生效。
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(QuartzProperties.class)
@ConditionalOnClass(Scheduler.class)
@ConditionalOnProperty(prefix = "platform.quartz", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@MapperScan("com.aurora.starter.quartz.mapper")
public class QuartzAutoConfiguration implements ApplicationContextAware {

    private static final String DDL_QUARTZ_JOB = """
            CREATE TABLE IF NOT EXISTS `quartz_job` (
                `job_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                `job_name`        VARCHAR(64)  NOT NULL COMMENT '任务名称',
                `job_group`       VARCHAR(64)           DEFAULT 'DEFAULT' COMMENT '任务分组',
                `cron_expression` VARCHAR(255) NOT NULL COMMENT 'Cron表达式',
                `invoke_target`   VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
                `concurrent`      CHAR(1)               DEFAULT '0' COMMENT '是否并发',
                `misfire_policy`  CHAR(1)               DEFAULT '0' COMMENT 'misfire策略',
                `status`          CHAR(1)               DEFAULT '0' COMMENT '状态',
                PRIMARY KEY (`job_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表'
            """;

    private static final String DDL_QUARTZ_JOB_LOG = """
            CREATE TABLE IF NOT EXISTS `quartz_job_log` (
                `log_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                `job_id`         BIGINT                DEFAULT NULL COMMENT '任务ID',
                `job_name`       VARCHAR(64)           DEFAULT NULL COMMENT '任务名称',
                `job_group`      VARCHAR(64)           DEFAULT NULL COMMENT '任务分组',
                `invoke_target`  VARCHAR(500)          DEFAULT NULL COMMENT '调用目标字符串',
                `start_time`     DATETIME              DEFAULT NULL COMMENT '开始时间',
                `stop_time`      DATETIME              DEFAULT NULL COMMENT '结束时间',
                `cost_millis`    BIGINT                DEFAULT NULL COMMENT '耗时(毫秒)',
                `job_message`    VARCHAR(500)          DEFAULT NULL COMMENT '任务消息',
                `status`         CHAR(1)               DEFAULT '0' COMMENT '执行状态',
                `exception_info` TEXT                  COMMENT '异常信息',
                PRIMARY KEY (`log_id`),
                KEY `idx_job_id` (`job_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表'
            """;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        JobInvokeUtils.applicationContext = ctx;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobLogHandler jobLogHandler(QuartzJobLogMapper logMapper, QuartzProperties properties) {
        return new DbJobLogHandler(logMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduleManager scheduleManager(Scheduler scheduler, QuartzProperties properties,
                                           JobLogHandler jobLogHandler) {
        return new DefaultScheduleManager(scheduler, properties, jobLogHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public IQuartzJobService quartzJobService(QuartzJobMapper jobMapper, ScheduleManager scheduleManager) {
        return new QuartzJobServiceImpl(scheduleManager, jobMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobBootstrap jobBootstrap(IQuartzJobService quartzJobService, ScheduleManager scheduleManager) {
        return new JobBootstrap(quartzJobService, scheduleManager);
    }

    /**
     * 自动建表 Initializer —— 仅在 {@code platform.quartz.auto-init-table=true} 时执行.
     * <p>
     * 使用 {@link DataSource} 直连执行 DDL，通过 {@code CREATE TABLE IF NOT EXISTS} 保证幂等.
     */
    @Bean
    @ConditionalOnProperty(prefix = "platform.quartz", name = "auto-init-table", havingValue = "true")
    public QuartzTableInitializer quartzTableInitializer() {
        return new QuartzTableInitializer();
    }

    @Slf4j
    @RequiredArgsConstructor
    static class QuartzTableInitializer {

        @PostConstruct
        void init() {
            try {
                DataSource ds = JobInvokeUtils.applicationContext.getBean(DataSource.class);
                try (Connection conn = ds.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute(DDL_QUARTZ_JOB);
                    stmt.execute(DDL_QUARTZ_JOB_LOG);
                }
                log.info("[quartz-spring-boot-starter] 自动建表完成: quartz_job + quartz_job_log");
            } catch (Exception e) {
                log.error("[quartz-spring-boot-starter] 自动建表失败", e);
            }
        }
    }
}
