package com.aurora.starter.quartz.core.schedule;

import com.aurora.starter.quartz.config.QuartzProperties;
import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.util.CronUtils;
import com.aurora.starter.quartz.util.ScheduleUtils;
import com.aurora.starter.quartz.enums.JobStatus;
import com.aurora.starter.quartz.exception.TaskException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * DefaultScheduleManager 默认实现 —— 直接委托 {@link Scheduler}.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultScheduleManager implements ScheduleManager {

    /** 默认允许并发执行的标记值. */
    private static final String CONCURRENT_ALLOW = "0";

    private final Scheduler scheduler;
    private final QuartzProperties properties;
    private final JobLogHandler jobLogHandler;

    @Override
    public void createOrUpdateJob(JobContext job) throws SchedulerException, TaskException {
        if (job == null) {
            throw new IllegalArgumentException("JobContext must not be null");
        }
        applyDefaults(job);
        log.info("创建/更新定时任务 jobId={}, jobGroup={}, cron={}",
                job.getJobId(), job.getJobGroup(), job.getCronExpression());
        ScheduleUtils.createScheduleJob(scheduler, job, jobLogHandler);
    }

    @Override
    public void pauseJob(Long jobId, String jobGroup) throws SchedulerException {
        log.info("暂停任务 jobId={}, jobGroup={}", jobId, jobGroup);
        scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    public void resumeJob(Long jobId, String jobGroup) throws SchedulerException {
        log.info("恢复任务 jobId={}, jobGroup={}", jobId, jobGroup);
        scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    public void deleteJob(Long jobId, String jobGroup) throws SchedulerException {
        log.info("删除任务 jobId={}, jobGroup={}", jobId, jobGroup);
        scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    public void triggerJob(Long jobId, String jobGroup) throws SchedulerException {
        log.info("立即触发任务 jobId={}, jobGroup={}", jobId, jobGroup);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    public boolean checkExists(Long jobId, String jobGroup) throws SchedulerException {
        return scheduler.checkExists(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    public boolean isValidCron(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    private void applyDefaults(JobContext job) {
        if (job.getJobGroup() == null || job.getJobGroup().isEmpty()) {
            job.setJobGroup(properties.getDefaultJobGroup());
        }
        if (job.getMisfirePolicy() == null || job.getMisfirePolicy().isEmpty()) {
            job.setMisfirePolicy(properties.getDefaultMisfirePolicy().getValue());
        }
        if (job.getStatus() == null || job.getStatus().isEmpty()) {
            job.setStatus(JobStatus.NORMAL.getValue());
        }
        if (job.getConcurrent() == null || job.getConcurrent().isEmpty()) {
            job.setConcurrent(CONCURRENT_ALLOW);
        }
    }
}
