package com.aurora.starter.quartz.bootstrap;

import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.exception.TaskException;
import com.aurora.starter.quartz.service.IQuartzJobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * 启动时把 quartz_job 中状态正常的任务同步到内存 Quartz.
 * <p>
 * 由 {@code platform.quartz.bootstrap.enabled} 控制(默认 true).
 * 设置为 false 可关闭(例如纯单元测试环境).
 */
@Slf4j
@Order
@RequiredArgsConstructor
public class JobBootstrap implements ApplicationRunner {

    private final IQuartzJobService quartzJobService;
    private final ScheduleManager scheduleManager;

    @Override
    public void run(ApplicationArguments args) throws SchedulerException, TaskException {
        // 查询所有状态正常的任务(status = "0")
        var jobs = quartzJobService.list(
                new LambdaQueryWrapper<QuartzJob>().eq(QuartzJob::getStatus, "0"));
        if (jobs.isEmpty()) {
            log.info("[quartz-spring-boot-starter] quartz_job 无启用任务,跳过启动同步");
            return;
        }
        for (QuartzJob entity : jobs) {
            try {
                scheduleManager.createOrUpdateJob(JobContext.from(entity));
                log.info("[quartz-spring-boot-starter] 启动同步任务 jobId={}, jobName={}, cron={}",
                        entity.getJobId(), entity.getJobName(), entity.getCronExpression());
            } catch (Exception e) {
                log.error("[quartz-spring-boot-starter] 启动同步任务失败 jobId={}", entity.getJobId(), e);
            }
        }
    }
}
