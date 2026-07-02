package com.aurora.starter.quartz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态.
 */
@Getter
@AllArgsConstructor
public enum JobStatus {

    /** 正常. */
    NORMAL("0"),

    /** 暂停. */
    PAUSE("1");

    private final String value;
}