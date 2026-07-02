package com.aurora.starter.quartz.util;

import org.quartz.CronExpression;

import java.util.Date;

/**
 * Cron 表达式工具.
 */
public final class CronUtils {

    private CronUtils() {
    }

    /**
     * 校验 Cron 表达式是否有效.
     */
    public static boolean isValid(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    /**
     * 返回 Cron 表达式的错误描述;有效则返回 null.
     */
    public static String getInvalidMessage(String cronExpression) {
        try {
            new CronExpression(cronExpression);
            return null;
        } catch (Exception pe) {
            return pe.getMessage();
        }
    }

    /**
     * 返回 Cron 表达式的下次执行时间.
     */
    public static Date getNextExecution(String cronExpression) {
        try {
            CronExpression cron = new CronExpression(cronExpression);
            return cron.getNextValidTimeAfter(new Date(System.currentTimeMillis()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
