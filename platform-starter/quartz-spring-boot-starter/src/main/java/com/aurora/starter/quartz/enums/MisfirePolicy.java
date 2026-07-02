package com.aurora.starter.quartz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Cron 任务 misfire 策略.
 */
@Getter
@AllArgsConstructor
public enum MisfirePolicy {

    /** 默认. */
    DEFAULT("0"),

    /** 立即触发执行. */
    IGNORE_MISFIRES("1"),

    /** 触发一次执行. */
    FIRE_AND_PROCEED("2"),

    /** 不触发立即执行. */
    DO_NOTHING("3");

    private final String value;
}