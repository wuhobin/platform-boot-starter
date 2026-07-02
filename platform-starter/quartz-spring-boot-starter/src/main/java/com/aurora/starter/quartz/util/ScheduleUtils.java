package com.aurora.starter.quartz.util;

import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.core.job.QuartzDisallowConcurrentExecution;
import com.aurora.starter.quartz.core.job.QuartzJobExecution;
import com.aurora.starter.quartz.enums.JobStatus;
import com.aurora.starter.quartz.enums.MisfirePolicy;
import com.aurora.starter.quartz.exception.TaskException;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * Quartz 调度工具.
 */
public final class ScheduleUtils {

    private static final String TASK_CLASS_NAME_PREFIX = "TASK_CLASS_NAME";
    /** 并发允许标记 —— 与原 aurora-scheduler 兼容. */
    private static final String CONCURRENT_ALLOW = "0";

    private ScheduleUtils() {
    }

    private static Class<? extends Job> getQuartzJobClass(JobContext job) {
        boolean isConcurrent = CONCURRENT_ALLOW.equals(job.getConcurrent());
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey(TASK_CLASS_NAME_PREFIX + jobId, jobGroup);
    }

    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey(TASK_CLASS_NAME_PREFIX + jobId, jobGroup);
    }

    /**
     * 创建或更新定时任务.
     */
    public static void createScheduleJob(Scheduler scheduler, JobContext job, JobLogHandler jobLogHandler)
            throws SchedulerException, TaskException {
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(getJobKey(jobId, jobGroup))
                .build();

        CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronBuilder = handleCronScheduleMisfirePolicy(job, cronBuilder);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronBuilder)
                .build();

        jobDetail.getJobDataMap().put(JobContext.class.getName(), job);
        jobDetail.getJobDataMap().put(JobLogHandler.class.getName(), jobLogHandler);

        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }
        scheduler.scheduleJob(jobDetail, trigger);

        if (JobStatus.PAUSE.getValue().equals(job.getStatus())) {
            scheduler.pauseJob(getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 设置 misfire 策略.
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(JobContext job, CronScheduleBuilder cb)
            throws TaskException {
        String policy = job.getMisfirePolicy();
        if (policy == null) {
            return cb;
        }
        if (MisfirePolicy.DEFAULT.getValue().equals(policy)) {
            return cb;
        }
        if (MisfirePolicy.IGNORE_MISFIRES.getValue().equals(policy)) {
            return cb.withMisfireHandlingInstructionIgnoreMisfires();
        }
        if (MisfirePolicy.FIRE_AND_PROCEED.getValue().equals(policy)) {
            return cb.withMisfireHandlingInstructionFireAndProceed();
        }
        if (MisfirePolicy.DO_NOTHING.getValue().equals(policy)) {
            return cb.withMisfireHandlingInstructionDoNothing();
        }
        throw new TaskException(
                "The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks",
                TaskException.Code.CONFIG_ERROR);
    }
}