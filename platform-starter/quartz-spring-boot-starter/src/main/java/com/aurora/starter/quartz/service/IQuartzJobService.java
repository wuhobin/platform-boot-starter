package com.aurora.starter.quartz.service;

import com.aurora.starter.quartz.domain.QuartzJob;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quartz.SchedulerException;

/**
 * 定时任务业务接口 —— 写 quartz_job 表 + 同步到 Quartz.
 */
public interface IQuartzJobService extends IService<QuartzJob> {

    boolean createJob(QuartzJob job) throws SchedulerException, com.aurora.starter.quartz.exception.TaskException;

    boolean updateJob(QuartzJob job) throws SchedulerException, com.aurora.starter.quartz.exception.TaskException;

    boolean deleteJob(Long jobId, String jobGroup) throws SchedulerException;

    boolean pauseJob(Long jobId, String jobGroup) throws SchedulerException;

    boolean resumeJob(Long jobId, String jobGroup) throws SchedulerException;

    boolean triggerNow(Long jobId, String jobGroup) throws SchedulerException;
}
