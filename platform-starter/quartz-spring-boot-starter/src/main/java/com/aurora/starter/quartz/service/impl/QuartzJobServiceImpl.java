package com.aurora.starter.quartz.service.impl;

import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.enums.JobStatus;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.quartz.mapper.QuartzJobMapper;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

@Slf4j
public class QuartzJobServiceImpl extends ServiceImpl<QuartzJobMapper, QuartzJob> implements IQuartzJobService {

    private final ScheduleManager scheduleManager;

    public QuartzJobServiceImpl(ScheduleManager scheduleManager, QuartzJobMapper baseMapper) {
        this.scheduleManager = scheduleManager;
        this.baseMapper = baseMapper;
    }

    @Override
    public boolean createJob(QuartzJob job) throws SchedulerException, TaskException {
        boolean saved = save(job);
        if (saved) {
            scheduleManager.createOrUpdateJob(JobContext.from(job));
            log.info("创建任务 jobId={}, jobName={}", job.getJobId(), job.getJobName());
        }
        return saved;
    }

    @Override
    public boolean updateJob(QuartzJob job) throws SchedulerException, TaskException {
        boolean updated = updateById(job);
        if (updated) {
            scheduleManager.createOrUpdateJob(JobContext.from(job));
            log.info("更新任务 jobId={}, jobName={}", job.getJobId(), job.getJobName());
        }
        return updated;
    }

    @Override
    public boolean deleteJob(Long jobId, String jobGroup) throws SchedulerException {
        boolean removed = removeById(jobId);
        if (removed) {
            scheduleManager.deleteJob(jobId, jobGroup);
            log.info("删除任务 jobId={}, jobGroup={}", jobId, jobGroup);
        }
        return removed;
    }

    @Override
    public boolean pauseJob(Long jobId, String jobGroup) throws SchedulerException {
        QuartzJob job = getById(jobId);
        if (job == null) {
            return false;
        }
        job.setStatus(JobStatus.PAUSE.getValue());
        boolean updated = updateById(job);
        if (!updated) {
            return false;
        }
        try {
            scheduleManager.pauseJob(jobId, jobGroup);
        } catch (Exception e) {
            // 回滚 DB 状态：Quartz 失败时把 DB 恢复到正常，防止状态不一致
            job.setStatus(JobStatus.NORMAL.getValue());
            updateById(job);
            throw e;
        }
        log.info("暂停任务 jobId={}, jobGroup={}", jobId, jobGroup);
        return true;
    }

    @Override
    public boolean resumeJob(Long jobId, String jobGroup) throws SchedulerException {
        QuartzJob job = getById(jobId);
        if (job == null) {
            return false;
        }
        job.setStatus(JobStatus.NORMAL.getValue());
        boolean updated = updateById(job);
        if (!updated) {
            return false;
        }
        try {
            scheduleManager.resumeJob(jobId, jobGroup);
        } catch (Exception e) {
            // Quartz 恢复失败时回滚 DB 写入
            job.setStatus(JobStatus.PAUSE.getValue());
            updateById(job);
            throw e;
        }
        log.info("恢复任务 jobId={}, jobGroup={}", jobId, jobGroup);
        return true;
    }

    @Override
    public boolean triggerNow(Long jobId, String jobGroup) throws SchedulerException {
        scheduleManager.triggerJob(jobId, jobGroup);
        log.info("立即触发任务 jobId={}, jobGroup={}", jobId, jobGroup);
        return true;
    }
}
