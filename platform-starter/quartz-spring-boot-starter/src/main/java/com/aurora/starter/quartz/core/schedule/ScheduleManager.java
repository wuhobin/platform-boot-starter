package com.aurora.starter.quartz.core.schedule;

import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.exception.TaskException;
import org.quartz.SchedulerException;

/**
 * 调度管理服务 —— 封装 Quartz Scheduler 复杂 API.
 */
public interface ScheduleManager {

    /**
     * 创建或更新任务(已存在则删除后重建).
     */
    void createOrUpdateJob(JobContext jobContext) throws TaskException, SchedulerException;

    /**
     * 暂停任务.
     */
    void pauseJob(Long jobId, String jobGroup) throws SchedulerException;

    /**
     * 恢复任务.
     */
    void resumeJob(Long jobId, String jobGroup) throws SchedulerException;

    /**
     * 删除任务(同时清 Trigger).
     */
    void deleteJob(Long jobId, String jobGroup) throws SchedulerException;

    /**
     * 立即触发一次(不破坏原 Cron).
     */
    void triggerJob(Long jobId, String jobGroup) throws SchedulerException;

    /**
     * 任务是否存在.
     */
    boolean checkExists(Long jobId, String jobGroup) throws SchedulerException;

    /**
     * 校验 Cron 表达式.
     */
    boolean isValidCron(String cronExpression);
}
