package com.aurora.starter.quartz.core.handler.impl;

import com.aurora.starter.quartz.config.QuartzProperties;
import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobLogRecord;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 默认 JobLogHandler —— 仅打印到 SLF4J,不写库.
 * <p>
 * 不使用 {@code @Component},完全由 {@code QuartzAutoConfiguration} 通过
 * {@code @ConditionalOnMissingBean} 注册,业务方提供自定义 Bean 时可干净覆盖.
 */
@RequiredArgsConstructor
public class LoggingJobLogHandler implements JobLogHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingJobLogHandler.class);

    private final QuartzProperties properties;

    @Override
    public void onSuccess(JobLogRecord record) {
        if (shouldSkip(record)) {
            return;
        }
        log.info("[Quartz Job Success] jobId={}, jobName={}, cost={}ms, msg={}",
                record.getJobId(), record.getJobName(),
                record.getCostMillis(), record.getJobMessage());
    }

    @Override
    public void onError(JobLogRecord record, Throwable error) {
        if (shouldSkip(record)) {
            return;
        }
        log.error("[Quartz Job Error] jobId={}, jobName={}, cost={}ms, exception={}",
                record.getJobId(), record.getJobName(),
                record.getCostMillis(), record.getExceptionInfo(), error);
    }

    private boolean shouldSkip(JobLogRecord record) {
        List<String> skipList = properties.getLog().getSkipList();
        if (skipList == null || skipList.isEmpty() || record.getInvokeTarget() == null) {
            return false;
        }
        for (String keyword : skipList) {
            if (record.getInvokeTarget().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
