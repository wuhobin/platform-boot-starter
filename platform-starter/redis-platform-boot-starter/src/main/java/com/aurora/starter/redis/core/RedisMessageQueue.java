package com.aurora.starter.redis.core;


import com.aurora.starter.redis.model.Message;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis 消息监听.
 *
 * @author whb
 * @date 2026/6/18
 */
@Slf4j
@AllArgsConstructor
public class RedisMessageQueue {

    private static final String LISTENER_KEY = "MESSAGE_LISTENER:%s";

    private final ConcurrentMap<String, RBlockingQueue<?>> queueCache = new ConcurrentHashMap<>();

    private RedissonClient redissonClient;

    /**
     * 发送消息到指定分组
     * @param group 分组
     * @param msg 消息内容
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> void send(final String group, final Message<T> msg) {
        RBlockingQueue<Message<T>> blockingQueue = getOrCreateQueue(group);
        log.info("Redis消息阻塞队列开始生产:[{} -> {}]【{}】", group, msg.getMsgId(), msg.getData());
        blockingQueue.putAsync(msg);
    }

    /**
     * 从指定分组消息获取消息，最多等待5秒
     * @param group 分组
     * @return 消息内容
     * @param <T>
     */
    public <T> Message<T> poll(final String group) {
        return this.poll(group, 5);
    }

    /**
     * 从指定分组消息获取消息
     * @param group 分组
     * @param timeout 超时时长（秒）
     * @return 消息内容
     * @param <T>
     */
    public <T> Message<T> poll(final String group, final long timeout) {
        return this.poll(group, timeout, TimeUnit.SECONDS);
    }

    /**
     * 从指定分组消息获取消息
     * @param group 分组
     * @param timeout 超时时长
     * @param unit 超时时长单位
     * @return 消息内容
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> Message<T> poll(final String group, final long timeout, final TimeUnit unit) {
        RBlockingQueue<Message<T>> blockingQueue = getOrCreateQueue(group);
        try {
            Message<T> message = blockingQueue.poll(timeout, unit);
            if (Objects.isNull(message)) {
                log.debug("从Redis消息阻塞队列获取消息超时[{}][{} {}]", group, timeout, unit.name());
                return null;
            }
            log.info("从Redis消息阻塞队列获取到消息:[{} -> {}]【{}】", group, message.getMsgId(), message.getData());
            return message;
        } catch (Exception e) {
            log.error("Redis消息阻塞队列消费异常[{}]：", group, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> RBlockingQueue<Message<T>> getOrCreateQueue(final String group) {
        return (RBlockingQueue<Message<T>>) queueCache.computeIfAbsent(group,
            g -> redissonClient.getBlockingQueue(String.format(LISTENER_KEY, g)));
    }
}
