package com.aurora.starter.redis.core;

import com.aurora.starter.common.utils.RedisKeyUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub 发布订阅.
 * <p>
 * 基于 Redisson RTopic，提供 push 模式的实时消息收发。
 * 每个 topic 可注册多个 listener，返回 Subscription 可控取消。
 * <p>
 * 对外暴露的 topic 是逻辑名，内部传给 Redisson 的 RTopic 会按
 * {@link #TOPIC_KEY_PREFIX} 统一加前缀，避免与业务自身 key 冲突。
 *
 * @author whb
 */
@Slf4j
public class RedisPubSub {

    /** 内部 topic 前缀，与 RedisKeyUtil.DELIMITER 拼接形成 "pubsub:topic:{name}". */
    public static final String TOPIC_KEY_PREFIX = "pubsub:topic";

    private final RedissonClient redissonClient;
    private final Map<String, TopicEntry> topics = new ConcurrentHashMap<>();

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
        return getOrCreate(topic).topic.publish(message);
    }

    private TopicEntry getOrCreate(String topic) {
        return topics.computeIfAbsent(topic, k -> new TopicEntry(redissonClient.getTopic(toRedissonKey(k))));
    }

    /**
     * 把对外的逻辑 topic 名加上内部前缀，生成传给 Redisson 的真实 key.
     */
    private static String toRedissonKey(String topic) {
        return RedisKeyUtil.generate(TOPIC_KEY_PREFIX, topic);
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
        TopicEntry entry = getOrCreate(topic);
        int listenerId = entry.topic.addListener(messageType, (channel, msg) -> {
            try {
                listener.accept(msg);
            } catch (Exception e) {
                log.error("Pub/Sub listener error on topic [{}]", topic, e);
            }
        });
        entry.listenerIds.add(listenerId);
        log.info("Pub/Sub subscribed to topic [{}] (listenerId={})", topic, listenerId);
        return new Subscription(topic, listenerId);
    }

    /**
     * 取消某个 topic 下的所有订阅.
     *
     * @param topic topic 名称
     */
    public void unsubscribeTopic(String topic) {
        TopicEntry entry = topics.remove(topic);
        if (entry != null) {
            removeAllListeners(topic, entry);
            log.info("Pub/Sub unsubscribed all listeners from topic [{}]", topic);
        }
    }

    @PreDestroy
    public void destroy() {
        // 先快照再清空，避免与并发 unsubscribeTopic() 在遍历中互相干扰
        List<Map.Entry<String, TopicEntry>> snapshot = new ArrayList<>(topics.entrySet());
        topics.clear();
        for (Map.Entry<String, TopicEntry> e : snapshot) {
            removeAllListeners(e.getKey(), e.getValue());
        }
        log.info("Pub/Sub all listeners removed");
    }

    private void removeAllListeners(String topic, TopicEntry entry) {
        for (int id : entry.listenerIds) {
            try {
                entry.topic.removeListener(id);
            } catch (Exception e) {
                log.warn("Pub/Sub removeListener failed for topic [{}] listenerId={}", topic, id, e);
            }
        }
    }

    /**
     * 取消单条订阅并清理已空的 topic 条目.
     * 由 Subscription.unsubscribe() 调用.
     */
    private void detach(String topicName, int listenerId) {
        topics.computeIfPresent(topicName, (k, entry) -> {
            try {
                entry.topic.removeListener(listenerId);
            } catch (Exception e) {
                log.warn("Pub/Sub removeListener failed for topic [{}] listenerId={}", topicName, listenerId, e);
            }
            entry.listenerIds.remove(Integer.valueOf(listenerId));
            return entry.listenerIds.isEmpty() ? null : entry;
        });
    }

    /**
     * 单个 topic 的缓存：复用 RTopic，并使用 CopyOnWriteArrayList
     * 存储 listenerId（写少读多，遍历无需加锁）.
     */
    private static final class TopicEntry {
        final RTopic topic;
        final CopyOnWriteArrayList<Integer> listenerIds = new CopyOnWriteArrayList<>();

        TopicEntry(RTopic topic) {
            this.topic = topic;
        }
    }

    /**
     * 订阅句柄，用于取消单次订阅.
     */
    public class Subscription {

        private final String topicName;
        private final int listenerId;

        private Subscription(String topicName, int listenerId) {
            this.topicName = topicName;
            this.listenerId = listenerId;
        }

        /**
         * 取消本次订阅.
         */
        public void unsubscribe() {
            detach(topicName, listenerId);
        }
    }
}