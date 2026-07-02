package com.aurora.starter.quartz.handler.impl;

import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobLogRecord;
import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 JobLogHandler —— 写 quartz_job_log 表.
 * <p>
 * 业务方可提供自定义 {@code JobLogHandler} Bean 覆盖此实现.
 */
@Slf4j
@RequiredArgsConstructor
public class DbJobLogHandler implements JobLogHandler {

    private final QuartzJobLogMapper logMapper;

    @Override
    public void onSuccess(JobLogRecord record) {
        QuartzJobLog entity = toEntity(record, "0");
        logMapper.insert(entity);
    }

    @Override
    public void onError(JobLogRecord record, Throwable error) {
        QuartzJobLog entity = toEntity(record, "1");
        logMapper.insert(entity);
        log.error("任务执行失败 jobId={}, jobName={}", record.getJobId(), record.getJobName(), error);
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
}
