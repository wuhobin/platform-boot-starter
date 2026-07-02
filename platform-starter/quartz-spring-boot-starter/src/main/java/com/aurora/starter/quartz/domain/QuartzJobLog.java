package com.aurora.starter.quartz.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务执行日志.
 */
@Data
@TableName("quartz_job_log")
public class QuartzJobLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志 ID. */
    @TableId(type = IdType.ASSIGN_ID)
    private Long logId;

    /** 任务 ID. */
    private Long jobId;

    /** 任务名. */
    private String jobName;

    /** 任务分组. */
    private String jobGroup;

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
