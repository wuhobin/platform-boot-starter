package com.aurora.starter.redis.core.manager;

import cn.hutool.core.thread.NamedThreadFactory;
import com.aurora.starter.common.utils.threads.RunnableDecorator;
import com.aurora.starter.common.utils.threads.ThreadsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务管理器
 *
 * @author author
 */
public class AsyncManager {

    /**
     * 核心线程池大小.
     */
    private static final int CORE_POOL_SIZE = 4;
    /**
     * 最大线程池大小
     */
    private static final int MAX_SIZE = 10;
    /**
     * 任务队列大小
     */
    private static final int QUEUE_CAPACITY = 1000;
    /**
     * 空闲线程存活时间
     */
    private static final int ALIVE_TIME = 2000;

    /**
     * 线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "schedule-pool-";

    private static final Logger LOG = LoggerFactory.getLogger("async-manager");
    /**
     * 异步操作任务调度线程池
     */
    private static final ThreadPoolExecutor executor;

    static {
        executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_SIZE, ALIVE_TIME, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new NamedThreadFactory(THREAD_NAME_PREFIX, null, true,
                (t, e) -> LOG.error("异步任务线程[{}}]抛出异常:", t.getName(), e)),
            new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void beforeExecute(final Thread t, final Runnable r) {
                super.beforeExecute(t, r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                ThreadsUtil.printException(r, t);
            }
        };
    }
    /**
     * 单例模式
     */
    private AsyncManager() {
    }

    private static final AsyncManager me = new AsyncManager();

    public static AsyncManager me() {
        return me;
    }

    /**
     * 执行任务
     *
     */
    public void execute(Runnable exec) {
        submit(exec);
    }

    /**
     * 执行任务
     *
     */
    public Future<?> submit(Runnable exec) {
        Runnable runnable = RunnableDecorator.decorate(exec, THREAD_NAME_PREFIX);
        return executor.submit(runnable);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown() {
        ThreadsUtil.shutdownAndAwaitTermination(executor);
    }

}
