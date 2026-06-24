package com.aurora.starter.redis.core.task;

import com.aurora.starter.redis.core.manager.AsyncManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * redis延迟队列实现.
 *
 * @author zenghao
 * @date 2022/6/23
 */
@Slf4j
public abstract class DelayedTask<T> implements InitializingBean {

    @Autowired
    private RedissonClient redissonClient;

    private RDelayedQueue<T> delayedQueue;

    private RBlockingQueue<T> blockingQueue;

    private int listenerId;

    @Override
    public void afterPropertiesSet() {
        this.blockingQueue = redissonClient.getBlockingQueue(getTaskGroup());
        this.delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        this.listenerId = subscribe();
    }

    /**
     * 任务分组名
     * @return 任务分组名
     */
    public abstract String getTaskGroup();

    /**
     * 消费数据
     * @param data 任务数据
     */
    public abstract void consumer(T data);

    /**
     * 当前队列数据长度
     * @return
     */
    public int size() {
        return this.blockingQueue.size();
    }

    /**
     * 是否同步执行
     *  都会将任务放如线程池执行，同步会等待执行结果，非同步则不等待
     * @return
     */
    public boolean isSync() {
        return false;
    }

    /**
     * 重新订阅 先取消之前的监听，再重新监听
     * @return 监听器id
     */
    public int reSubscribe() {
        int oldId = this.listenerId;
        blockingQueue.unsubscribe(oldId);
        this.listenerId = subscribe();
        log.info("延时队列重新订阅消费[{}]: 【{} -> {}】", getTaskGroup(), oldId, listenerId);
        return this.listenerId;
    }

    /**
     * 生产消息
     * @param data 消息数据
     * @param delay 延迟时间 单位：秒
     */
    public void producer(T data, long delay) {
        log.info("延时队列【{}】插入延时[{}秒]数据:{}", getTaskGroup(), delay, data);
        delayedQueue.offerAsync(data, delay, TimeUnit.SECONDS);
    }

    /**
     * 处理异常
     * @param data 消费数据
     * @param e 异常信息
     */
    protected void handleException(T data, Exception e) {
        //do something for handle exception
    }

    /**
     * 开启订阅队列消息
     * @return 监听器id
     */
    private int subscribe() {
        return blockingQueue.subscribeOnElements(data -> {
                try {
                    Future<?> future = subscribeElement(data);
                    if (isSync()) {
                        future.get();
                    }
                } catch (Exception e) {
                    log.error("延时队列消费异常【{}】【{}】:", getTaskGroup(), data, e);
                    handleException(data, e);
                }
            }
        );
    }

    protected Future<?> subscribeElement(T data) {
        return AsyncManager.me().submit(() -> {
            log.info("延时队列开始消费【{}】:{}", getTaskGroup(), data);
            consumer(data);
            log.info("延时队列完成消费【{}】:{}", getTaskGroup(), data);
        });
    }
}
