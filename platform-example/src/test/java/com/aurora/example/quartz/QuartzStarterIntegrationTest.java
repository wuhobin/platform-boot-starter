package com.aurora.example.quartz;

import com.aurora.starter.quartz.core.job.JobContext;
import com.aurora.starter.quartz.core.schedule.ScheduleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * quartz-spring-boot-starter 端到端集成测试.
 */
@SpringBootTest
@ActiveProfiles("quartz-test")
@Import(QuartzTestConfig.class)
@DisplayName("Quartz Starter 集成测试")
class QuartzStarterIntegrationTest {

    @Autowired
    private ScheduleManager scheduleManager;

    @Autowired
    private Scheduler scheduler;

    @BeforeEach
    void setUp() {
        TestTask.reset();
        InMemoryJobLogHandler.reset();
    }

    @Test
    @DisplayName("Starter 装配成功 —— ScheduleManager 与 Scheduler 注入正常")
    void shouldAutoConfigureScheduleManager() {
        assertNotNull(scheduleManager);
        assertNotNull(scheduler);
    }

    @Test
    @DisplayName("createOrUpdateJob —— 任务被 Quartz 注册并可触发")
    void shouldCreateAndTriggerJob() throws Exception {
        JobContext job = JobContext.builder()
                .jobId(9001L)
                .jobGroup("DEFAULT")
                .jobName("testTask")
                .cronExpression("0/1 * * * * ?")
                .invokeTarget("testTask.doExecute()")
                .concurrent("0")
                .misfirePolicy("0")
                .status("0")
                .build();

        scheduleManager.createOrUpdateJob(job);

        assertTrue(scheduleManager.checkExists(9001L, "DEFAULT"));
        scheduleManager.triggerJob(9001L, "DEFAULT");

        await().atMost(5, TimeUnit.SECONDS).until(() -> TestTask.invokeCount >= 1);

        scheduleManager.deleteJob(9001L, "DEFAULT");
        assertFalse(scheduleManager.checkExists(9001L, "DEFAULT"));
    }

    @Test
    @DisplayName("JobLogHandler SPI 覆盖 —— 任务执行记录进入自定义 Handler")
    void shouldInvokeCustomJobLogHandler() throws Exception {
        JobContext job = JobContext.builder()
                .jobId(9002L)
                .jobGroup("DEFAULT")
                .jobName("testTask")
                .cronExpression("0/1 * * * * ?")
                .invokeTarget("testTask.doExecute()")
                .status("0")
                .build();

        scheduleManager.createOrUpdateJob(job);
        scheduleManager.triggerJob(9002L, "DEFAULT");

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> !InMemoryJobLogHandler.RECORDS.isEmpty());

        var record = InMemoryJobLogHandler.RECORDS.get(0);
        assertEquals(9002L, record.getJobId());
        assertEquals("testTask", record.getJobName());
        assertEquals("0", record.getStatus());

        scheduleManager.deleteJob(9002L, "DEFAULT");
    }

    @Test
    @DisplayName("isValidCron —— 校验 Cron 表达式")
    void shouldValidateCron() {
        assertTrue(scheduleManager.isValidCron("0/5 * * * * ?"));
        assertFalse(scheduleManager.isValidCron("not-a-cron"));
    }
}