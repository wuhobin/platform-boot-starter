package com.aurora.example.controller.quartz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例任务 Bean —— 演示业务方自定义任务（只需 @Component + 普通方法,不需继承任何 starter 类）.
 * <p>
 * 配合 {@code quartz_job.invoke_target = "exampleTask.doSomething()"} 使用.
 */
@Slf4j
@Component("exampleTask")
public class ExampleTask {

    /**
     * 无参方法 —— invoke_target 写 "exampleTask.doSomething".
     */
    public void doSomething() {
        log.info("[ExampleTask] doSomething 执行中...");
    }

    /**
     * 带参方法(测试 invokeTarget 字符串解析) —— invoke_target 写 "exampleTask.doWithParams('hello',100L)".
     */
    public void doWithParams(String name, Long count) {
        log.info("[ExampleTask] doWithParams name={}, count={}", name, count);
    }
}
