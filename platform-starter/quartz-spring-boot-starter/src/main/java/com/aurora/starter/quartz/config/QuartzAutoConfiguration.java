package com.aurora.starter.quartz.config;

import com.aurora.starter.quartz.bootstrap.JobBootstrap;
import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.schedule.DefaultScheduleManager;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import com.aurora.starter.quartz.handler.impl.DbJobLogHandler;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import com.aurora.starter.quartz.mapper.QuartzJobMapper;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.aurora.starter.quartz.service.impl.QuartzJobServiceImpl;
import com.aurora.starter.quartz.util.JobInvokeUtils;
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
}
