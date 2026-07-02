package com.aurora.starter.quartz.core.handler.impl;

import com.aurora.starter.quartz.config.QuartzProperties;
import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobLogRecord;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认 JobLogHandler —— 写 quartz_job_log 表.
 * <p>
 * 通过 {@code platform.quartz.log.*} 控制行为:
 * <ul>
 *   <li>{@code max-exception-length} — 异常信息截断长度(默认 2000)</li>
 *   <li>{@code skip-list} — invokeTarget 关键字列表,命中则跳过日志</li>
 * </ul>
 * 业务方可提供自定义 {@code JobLogHandler} Bean 覆盖此实现.
 */
@Slf4j
@RequiredArgsConstructor
public class DbJobLogHandler implements JobLogHandler {

    private final QuartzJobLogMapper logMapper;
    private final QuartzProperties properties;

    @Override
    public void onSuccess(JobLogRecord record) {
        if (shouldSkip(record)) {
            return;
        }
        logMapper.insert(toEntity(record, "0"));
    }

    @Override
    public void onError(JobLogRecord record, Throwable error) {
        if (shouldSkip(record)) {
            return;
        }
        QuartzJobLog entity = toEntity(record, "1");
        entity.setExceptionInfo(truncate(entity.getExceptionInfo(), properties.getLog().getMaxExceptionLength()));
        logMapper.insert(entity);
        log.error("任务执行失败 jobId={}, jobName={}", record.getJobId(), record.getJobName(), error);
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

    private QuartzJobLog toEntity(JobLogRecord r, String status) {
        QuartzJobLog entity = new QuartzJobLog();
        entity.setJobId(r.getJobId());
        entity.setJobGroup(r.getJobGroup());
        entity.setJobName(r.getJobName());
        entity.setInvokeTarget(r.getInvokeTarget());
        entity.setStartTime(r.getStartTime());
        entity.setStopTime(r.getStopTime());
        entity.setCostMillis(r.getCostMillis());
        entity.setJobMessage(r.getJobMessage());
        entity.setStatus(status);
        entity.setExceptionInfo(r.getExceptionInfo());
        return entity;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
