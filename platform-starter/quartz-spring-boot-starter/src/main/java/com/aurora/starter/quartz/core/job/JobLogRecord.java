package com.aurora.starter.quartz.core.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志 —— 替代业务实体 QuartzJobLog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLogRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务 ID. */
    private Long jobId;
    /** 任务分组. */
    private String jobGroup;
    /** 任务名. */
    private String jobName;
    /** 调用目标字符串. */
    private String invokeTarget;
    /** 开始时间. */
    private LocalDateTime startTime;
    /** 结束时间. */
    private LocalDateTime stopTime;
    /** 耗时(毫秒). */
    private Long costMillis;
    /** 任务消息. */
    private String jobMessage;
    /** "0"=成功,"1"=失败. */
    private String status;
    /** 异常信息. */
    private String exceptionInfo;
}