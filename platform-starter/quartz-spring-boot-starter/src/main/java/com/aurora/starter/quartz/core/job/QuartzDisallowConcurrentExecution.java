package com.aurora.starter.quartz.core.job;

import com.aurora.starter.quartz.util.JobInvokeUtils;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;

/**
 * 禁止并发的任务处理.
 */
@PersistJobDataAfterExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, JobContext job) throws Exception {
        JobInvokeUtils.invokeMethod(job);
    }
}
