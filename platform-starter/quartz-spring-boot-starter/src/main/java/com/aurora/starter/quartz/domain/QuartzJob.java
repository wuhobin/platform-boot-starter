package com.aurora.starter.quartz.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务实体.
 */
@Data
@TableName("quartz_job")
public class QuartzJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务 ID. */
    @TableId(type = IdType.ASSIGN_ID)
    private Long jobId;

    /** 任务名. */
    private String jobName;

    /** 任务分组. */
    private String jobGroup;

    /** Cron 表达式. */
    private String cronExpression;

    /** 调用目标字符串,如 "beanName.method('a',1L,2D)". */
    private String invokeTarget;

    /** 是否并发:"0"=允许,"1"=禁止. */
    private String concurrent;

    /** misfire 策略:"0"=DEFAULT,"1"=IGNORE_MISFIRES,"2"=FIRE_AND_PROCEED,"3"=DO_NOTHING. */
    private String misfirePolicy;

    /** 任务状态:"0"=正常,"1"=暂停. */
    private String status;
}
