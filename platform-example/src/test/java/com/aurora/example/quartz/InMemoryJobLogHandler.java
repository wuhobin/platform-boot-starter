package com.aurora.example.quartz;

import com.aurora.starter.quartz.core.handler.JobLogHandler;
import com.aurora.starter.quartz.core.job.JobLogRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用 JobLogHandler —— 覆盖 starter 默认实现,把日志写入内存列表.
 */
@TestConfiguration
public class InMemoryJobLogHandler {

    public static final List<JobLogRecord> RECORDS = new CopyOnWriteArrayList<>();
    public static volatile Throwable lastError;

    @Bean
    @Primary
    public JobLogHandler inMemoryJobLogHandler() {
        return new JobLogHandler() {
            @Override
            public void onSuccess(JobLogRecord record) {
                RECORDS.add(record);
            }

            @Override
            public void onError(JobLogRecord record, Throwable error) {
                lastError = error;
                RECORDS.add(record);
            }
        };
    }

    public static void reset() {
        RECORDS.clear();
        lastError = null;
    }
}