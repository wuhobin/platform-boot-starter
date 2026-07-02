package com.aurora.starter.quartz.core.handler;

import com.aurora.starter.quartz.core.job.JobLogRecord;

/**
 * 任务日志 SPI —— 业务方实现此接口并注册为 Spring Bean 即可接管日志持久化.
 */
public interface JobLogHandler {

    /**
     * 任务执行成功.
     */
    void onSuccess(JobLogRecord record);

    /**
     * 任务执行失败.
     */
    void onError(JobLogRecord record, Throwable error);
}
