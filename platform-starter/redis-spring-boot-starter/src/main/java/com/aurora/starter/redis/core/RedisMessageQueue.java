package com.aurora.starter.redis.core;

import com.aurora.starter.common.utils.RedisKeyUtil;
import com.aurora.starter.redis.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis 可靠消息队列.
 * <p>
 * 基于 Redisson {@link RBlockingQueue}，按 group 隔离；生产端异步投放，
 * 消费端阻塞拉取。{@link #send(String, Message)} 内部对异步结果做了异常回调，
 * 失败会写日志；调用方无需关心返回值。
 * <p>
 * <b>注意：</b>{@link #poll(String, long, TimeUnit)} 会阻塞当前线程直到超时或拿到消息，
 * 调用方必须从专用工作线程调用，禁止在 Tomcat/NIO 等请求线程上直接调用。
 *
 * @author whb
 * @date 2026/6/18
 */
@Slf4j
public class RedisMessageQueue {

    private static final String QUEUE_KEY_PREFIX = "MESSAGE_QUEUE";

    /** {@link #poll(String)} 默认阻塞时长（秒）. */
    private static final long DEFAULT_POLL_TIMEOUT_SECONDS = 5L;

    private final RedissonClient redissonClient;
    private final ConcurrentMap<String, RBlockingQueue<?>> queueCache = new ConcurrentHashMap<>();

    public RedisMessageQueue(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 发送消息到指定分组（异步投放，失败会写日志）.
     *
     * @param group 分组
     * @param msg   消息内容
     * @param <T>   消息数据类型
     */
    public <T> void send(final String group, final Message<T> msg) {
        RBlockingQueue<Message<T>> blockingQueue = queueFor(group);
        log.info("Redis消息阻塞队列开始生产:[{} -> {}]【{}】", group, msg.getMsgId(), msg.getData());
        // fire-and-forget：putAsync 失败时打日志，避免丢消息无感知
        blockingQueue.putAsync(msg).exceptionally(t -> {
            log.error("Redis消息阻塞队列生产异常[{} -> {}]【{}]：", group, msg.getMsgId(), msg.getData(), t);
            return null;
        });
    }

    /**
     * 从指定分组阻塞获取消息，默认等待 {@value #DEFAULT_POLL_TIMEOUT_SECONDS} 秒.
     *
     * @param group 分组
     * @param <T>   消息数据类型
     * @return 消息内容；超时或异常返回 {@code null}
     */
    public <T> Message<T> poll(final String group) {
        return this.poll(group, DEFAULT_POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 从指定分组阻塞获取消息.
     *
     * @param group   分组
     * @param timeout 超时时长
     * @param unit    超时时长单位
     * @param <T>     消息数据类型
     * @return 消息内容；超时或异常返回 {@code null}
     */
    public <T> Message<T> poll(final String group, final long timeout, final TimeUnit unit) {
        RBlockingQueue<Message<T>> blockingQueue = queueFor(group);
        try {
            Message<T> message = blockingQueue.poll(timeout, unit);
            if (message == null) {
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

    /**
     * 按 group 获取或创建对应的阻塞队列（缓存同名队列句柄）.
     * <p>
     * 唯一的 unchecked 转换点；外部调用方无需感知泛型擦除.
     */
    @SuppressWarnings("unchecked")
    private <T> RBlockingQueue<Message<T>> queueFor(final String group) {
        return (RBlockingQueue<Message<T>>) queueCache.computeIfAbsent(group,
            g -> redissonClient.getBlockingQueue(RedisKeyUtil.generate(QUEUE_KEY_PREFIX, g)));
    }
}