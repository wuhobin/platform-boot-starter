package com.aurora.starter.quartz.core.job;

import com.aurora.starter.quartz.core.handler.JobLogHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 抽象 Quartz Job —— 提供统一的 before/doExecute/after 流程.
 * <p>
 * 业务方继承该类并重写 {@link #doExecute(JobExecutionContext, JobContext)} 即可.
 */
public abstract class AbstractQuartzJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    private static final ThreadLocal<LocalDateTime> START_TIME = new ThreadLocal<>();

    private static final int DEFAULT_MAX_EXCEPTION_LENGTH = 2000;

    @Override
    public void execute(JobExecutionContext context) {
        JobContext job = (JobContext) context.getMergedJobDataMap().get(JobContext.class.getName());
        try {
            before(context, job);
            doExecute(context, job);
            handleAfter(context, job, null);
        } catch (Exception e) {
            log.error("任务执行异常 - {}", job != null ? job.getJobName() : "unknown", e);
            handleAfter(context, job, e);
        }
    }

    /**
     * 执行前钩子 —— 默认仅记录开始时间.
     */
    protected void before(JobExecutionContext context, JobContext job) {
        START_TIME.set(LocalDateTime.now());
    }

    /**
     * 子类实现具体业务逻辑.
     */
    protected abstract void doExecute(JobExecutionContext context, JobContext job) throws Exception;

    private void handleAfter(JobExecutionContext context, JobContext job, Exception error) {
        LocalDateTime start = START_TIME.get();
        START_TIME.remove();
        if (job == null || job.isSkipLog()) {
            return;
        }
        try {
            JobLogHandler handler = (JobLogHandler) context.getMergedJobDataMap().get(JobLogHandler.class.getName());
            if (handler == null) {
                log.warn("JobDataMap 中未找到 JobLogHandler，跳过日志处理 - {}", job.getJobName());
                return;
            }
            JobLogRecord record = buildRecord(job, start, error);
            if (error != null) {
                handler.onError(record, error);
            } else {
                handler.onSuccess(record);
            }
        } catch (Exception ex) {
            log.error("任务日志处理失败 - {}", job.getJobName(), ex);
        }
    }

    private JobLogRecord buildRecord(JobContext job, LocalDateTime start, Exception error) {
        long runMs = start != null
                ? Duration.between(start, LocalDateTime.now()).toMillis()
                : 0L;
        JobLogRecord.JobLogRecordBuilder builder = JobLogRecord.builder()
                .jobId(job.getJobId())
                .jobGroup(job.getJobGroup())
                .jobName(job.getJobName())
                .invokeTarget(job.getInvokeTarget())
                .startTime(start)
                .stopTime(LocalDateTime.now())
                .costMillis(runMs)
                .jobMessage(job.getJobName() + " 总共耗时：" + runMs + "毫秒");
        if (error != null) {
            builder.status("1")
                    .exceptionInfo(truncate(stackTraceOf(error), DEFAULT_MAX_EXCEPTION_LENGTH));
        } else {
            builder.status("0");
        }
        return builder.build();
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
