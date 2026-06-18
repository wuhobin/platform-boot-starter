package com.aurora.starter.redis.core.task;


import com.aurora.starter.redis.model.DelayRetry;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 延迟自动重试任务.
 *
 * @author zenghao
 * @date 2022/12/12
 */
@Slf4j
public abstract class DelayedRetryTask<T> extends DelayedTask<DelayRetry<T>> {

    protected ExecutorService executor;

    public DelayedRetryTask() {
    }

    public DelayedRetryTask(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    protected Future<?> subscribeElement(final DelayRetry<T> data) {
        if (Objects.isNull(executor)) {
            return super.subscribeElement(data);
        }
        return executor.submit(() -> consumer(data));
    }

    @Override
    public void consumer(final DelayRetry<T> task) {
        try {
            boolean success = execute(task.getData());
            if (!success) {
                log.warn("[RETRY]【{}】任务返回false, 触发重试", getTaskGroup());
                retry(task);
                return;
            }
            log.info("[RETRY]【{}】第[{}]次处理成功", getTaskGroup(), task.getCount());
        } catch (Exception e) {
            log.warn("[RETRY]【{}】处理异常:", getTaskGroup(), e);
            handleException(task, e);
            retry(task);
        }
    }

    private void retry(final DelayRetry<T> task) {
        task.addCount();
        if (task.checkAndIncrement()) {
            long delay = nextTime(task);
            log.info("[RETRY]【{}】准备间隔{}秒后进行第[{}]次重试", getTaskGroup(), delay, task.getCount());
            this.producer(task, delay);
        }
    }

    /**
     * 执行任务
     *
     * @param data 任务数据
     * @return 执行结果，返回false 或抛出异常则重试
     */
    protected abstract boolean execute(T data);

    /**
     * 自增并判断是否开启下一次重试.
     *
     * @param task 任务数据
     * @return 开启下一次重试
     */
    public boolean hasNext(final DelayRetry<T> task) {
        return task.checkAndIncrement();
    }

    /**
     * 下次重试时间 默认根据 重试次数*间隔时间
     * @param task 任务数据
     * @return 下次重试时间
     */
    public long nextTime(final DelayRetry<T> task) {
        return task.nextTime();
    }
}
