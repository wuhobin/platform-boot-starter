package com.aurora.starter.quartz.core.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务运行时上下文 —— 替代业务实体 QuartzJob.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务 ID. */
    private Long jobId;

    /** 任务分组. */
    private String jobGroup;

    /** 任务名. */
    private String jobName;

    /** Cron 表达式. */
    private String cronExpression;

    /** 调用目标字符串,如 "beanName.method('a',1L,2D)". */
    private String invokeTarget;

    /** 是否并发:"0"=允许,"1"=禁止. */
    private String concurrent;

    /** misfire 策略,见 MisfirePolicy.value. */
    private String misfirePolicy;

    /** 任务状态,见 JobStatus.value. */
    private String status;

    /** 是否跳过日志记录(替代原 redisTimer 字符串黑名单). */
    private boolean skipLog;

    /**
     * 从 {@link com.aurora.starter.quartz.domain.QuartzJob} 实体转换为运行时上下文.
     */
    public static JobContext from(com.aurora.starter.quartz.domain.QuartzJob e) {
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