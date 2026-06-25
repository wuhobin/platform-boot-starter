package com.aurora.starter.redis.core;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub 发布订阅.
 * <p>
 * 基于 Redisson RTopic，提供 push 模式的实时消息收发。
 * 每个 topic 可注册多个 listener，返回 Subscription 可控取消。
 *
 * @author whb
 */
@Slf4j
public class RedisPubSub {

    private final RedissonClient redissonClient;
    private final Map<String, List<Integer>> listenerIds = new ConcurrentHashMap<>();

    public RedisPubSub(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 发送消息到指定 topic.
     *
     * @param topic   topic 名称
     * @param message 消息体
     * @return 收到消息的订阅者数量
     */
    public long publish(String topic, Object message) {
        RTopic rtopic = redissonClient.getTopic(topic);
        return rtopic.publish(message);
    }

    /**
     * 订阅指定 topic.
     *
     * @param topic       topic 名称
     * @param messageType 消息类型
     * @param listener    消息处理器
     * @param <T>         消息类型
     * @return Subscription，可调用 unsubscribe() 取消
     */
    public <T> Subscription subscribe(String topic, Class<T> messageType, Consumer<T> listener) {
        RTopic rtopic = redissonClient.getTopic(topic);
        int listenerId = rtopic.addListener(messageType, (channel, msg) -> {
            try {
                listener.accept(msg);
            } catch (Exception e) {
                log.error("Pub/Sub listener error on topic [{}]", topic, e);
            }
        });
        listenerIds.computeIfAbsent(topic, k -> Collections.synchronizedList(new ArrayList<>())).add(listenerId);
        log.info("Pub/Sub subscribed to topic [{}] (listenerId={})", topic, listenerId);
        return new Subscription(rtopic, listenerId, topic, listenerIds);
    }

    /**
     * 取消某个 topic 下的所有订阅.
     *
     * @param topic topic 名称
     */
    public void unsubscribeTopic(String topic) {
        List<Integer> ids = listenerIds.remove(topic);
        if (ids != null) {
            synchronized (ids) {
                RTopic rtopic = redissonClient.getTopic(topic);
                for (int id : ids) {
                    try {
                        rtopic.removeListener(id);
                    } catch (Exception e) {
                        log.warn("Pub/Sub removeListener failed for topic [{}] listenerId={}", topic, id, e);
                    }
                }
            }
            log.info("Pub/Sub unsubscribed all listeners from topic [{}]", topic);
        }
    }

    @PreDestroy
    public void destroy() {
        listenerIds.forEach((topic, ids) -> {
            // 加锁遍历，避免与 unsubscribe() 中 ids.remove() 并发
            synchronized (ids) {
                RTopic rtopic = redissonClient.getTopic(topic);
                for (int id : ids) {
                    try {
                        rtopic.removeListener(id);
                    } catch (Exception e) {
                        // 容错：单个 listener 清理失败不影响其他 topic
                        log.warn("Pub/Sub removeListener failed for topic [{}] listenerId={}", topic, id, e);
                    }
                }
            }
        });
        listenerIds.clear();
        log.info("Pub/Sub all listeners removed");
    }

    /**
     * 订阅句柄，用于取消单次订阅.
     */
    public static class Subscription {

        private final RTopic topic;
        private final int listenerId;
        private final String topicName;
        private final Map<String, List<Integer>> listenerIds;

        private Subscription(RTopic topic, int listenerId, String topicName,
                             Map<String, List<Integer>> listenerIds) {
            this.topic = topic;
            this.listenerId = listenerId;
            this.topicName = topicName;
            this.listenerIds = listenerIds;
        }

        /**
         * 取消本次订阅.
         */
        public void unsubscribe() {
            topic.removeListener(listenerId);
            // 原子操作：get + remove 合并为 computeIfPresent
            // 内部对 ids 加 synchronized，与 destroy() 遍历共享同一把锁
            listenerIds.computeIfPresent(topicName, (k, ids) -> {
                synchronized (ids) {
                    ids.remove(Integer.valueOf(listenerId));
                }
                return ids.isEmpty() ? null : ids;
            });
        }
    }
}
