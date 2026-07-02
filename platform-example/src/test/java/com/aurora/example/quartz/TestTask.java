package com.aurora.example.quartz;

import org.springframework.stereotype.Component;

/**
 * 用于集成测试的任务 Bean —— 暴露零参 doExecute() 方法,
 * 由 starter 通过 invokeTarget="testTask.doExecute()" 反射调用.
 *
 * <p>Quartz 调度器始终以 {@code QuartzJobExecution} 作为 Job class
 * (由 {@code ScheduleUtils.createScheduleJob} 写入),并通过 SpringBeanJobFactory
 * 直接实例化 QuartzJobExecution.实际调用链:
 * {@code QuartzJobExecution#execute → QuartzJobExecution#doExecute → JobInvokeUtils#invokeMethod}
 * 后者通过 invokeTarget 反射调用本类的零参 {@code doExecute()}.
 */
@Component("testTask")
public class TestTask {

    public static volatile int invokeCount = 0;

    /**
     * 零参 doExecute —— 由 starter 通过 invokeTarget="testTask.doExecute()" 反射调用.
     */
    public void doExecute() {
        invokeCount++;
    }

    public static void reset() {
        invokeCount = 0;
    }
}