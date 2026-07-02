package com.aurora.starter.quartz.core.job;

import com.aurora.starter.quartz.util.JobInvokeUtils;
import org.quartz.JobExecutionContext;

/**
 * 允许并发的任务处理.
 */
public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, JobContext job) throws Exception {
        JobInvokeUtils.invokeMethod(job);
    }
}
