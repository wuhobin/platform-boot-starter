package com.aurora.starter.quartz.service.impl;

import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import com.aurora.starter.quartz.domain.QuartzJob;
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
            scheduleManager.createOrUpdateJob(toContext(job));
            log.info("创建任务 jobId={}, jobName={}", job.getJobId(), job.getJobName());
        }
        return saved;
    }

    @Override
    public boolean updateJob(QuartzJob job) throws SchedulerException, TaskException {
        boolean updated = updateById(job);
        if (updated) {
            scheduleManager.createOrUpdateJob(toContext(job));
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
        job.setStatus("1");
        boolean updated = updateById(job);
        if (updated) {
            scheduleManager.pauseJob(jobId, jobGroup);
            log.info("暂停任务 jobId={}, jobGroup={}", jobId, jobGroup);
        }
        return updated;
    }

    @Override
    public boolean resumeJob(Long jobId, String jobGroup) throws SchedulerException {
        QuartzJob job = getById(jobId);
        if (job == null) {
            return false;
        }
        job.setStatus("0");
        boolean updated = updateById(job);
        if (updated) {
            scheduleManager.resumeJob(jobId, jobGroup);
            log.info("恢复任务 jobId={}, jobGroup={}", jobId, jobGroup);
        }
        return updated;
    }

    @Override
    public boolean triggerNow(Long jobId, String jobGroup) throws SchedulerException {
        scheduleManager.triggerJob(jobId, jobGroup);
        log.info("立即触发任务 jobId={}, jobGroup={}", jobId, jobGroup);
        return true;
    }

    private JobContext toContext(QuartzJob e) {
        return JobContext.builder()
                .jobId(e.getJobId())
                .jobGroup(e.getJobGroup())
                .jobName(e.getJobName())
                .cronExpression(e.getCronExpression())
                .invokeTarget(e.getInvokeTarget())
                .concurrent(e.getConcurrent())
                .misfirePolicy(e.getMisfirePolicy())
                .status(e.getStatus())
                .build();
    }
}
