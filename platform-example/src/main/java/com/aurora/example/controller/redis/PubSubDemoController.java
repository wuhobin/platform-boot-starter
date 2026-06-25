package com.aurora.example.controller.redis;

import com.aurora.starter.redis.core.RedisPubSub;
import com.aurora.starter.webmvc.domain.response.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Redis Pub/Sub 功能演示.
 * <p>
 * 应用启动时自动订阅 {@link #DEFAULT_TOPIC}，收到的消息存入内存队列。
 * 支持同时订阅多个 topic，每个 topic 独立维护 {@link RedisPubSub.Subscription} 句柄。
 * 完整演示链路：
 * <pre>
 *   1. POST /pubsub/publish?topic=demo:event&message=hello    # 发布
 *   2. GET  /pubsub/subscribe?topic=demo:event                 # 订阅（幂等：重复订阅返回已订阅）
 *   3. GET  /pubsub/subscribe?topic=order:event               # 订阅第二个 topic
 *   4. GET  /pubsub/received                                  # 查看收到的所有消息
 *   5. DELETE /pubsub/unsubscribe?topic=demo:event             # 取消单个 topic 的订阅
 *   6. POST /pubsub/unsubscribeAll                            # 取消所有订阅
 *   7. GET  /pubsub/status                                    # 查看当前活跃订阅列表
 * </pre>
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/pubsub")
public class PubSubDemoController {

    /** 启动时自动订阅的默认 topic. */
    private static final String DEFAULT_TOPIC = "demo:event";

    /** 内存中保留的最近消息数. */
    private static final int MAX_RETAINED = 100;

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Autowired
    private RedisPubSub redisPubSub;

    /** 当前活跃订阅：topic → Subscription 句柄. */
    private final Map<String, RedisPubSub.Subscription> subscriptions = new ConcurrentHashMap<>();

    /** 收到的消息队列（包含来源 topic + 时间戳 + 内容）. */
    private final Deque<ReceivedMessage> received = new ConcurrentLinkedDeque<>();

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        doSubscribe(DEFAULT_TOPIC);
    }

    @PreDestroy
    public void shutdown() {
        unsubscribeAllInternal("shutdown");
    }

    // ==================== 发布 ====================

    /**
     * 向指定 topic 发布消息.
     * <p>
     * GET /pubsub/publish?topic=demo:event&message=hello-world
     */
    @GetMapping("/publish")
    public Result<Long> publish(@RequestParam String topic,
                                 @RequestParam String message) {
        long receivers = redisPubSub.publish(topic, message);
        log.info("发布消息到 [{}]: {} (收到订阅者数量: {})", topic, message, receivers);
        return Result.data(receivers);
    }

    // ==================== 订阅控制 ====================

    /**
     * 订阅指定 topic；已订阅同一 topic 时返回幂等提示，不会重复注册 listener.
     * <p>
     * GET /pubsub/subscribe?topic=demo:event
     */
    @GetMapping("/subscribe")
    public Result<String> subscribe(@RequestParam String topic) {
        if (subscriptions.containsKey(topic)) {
            return Result.success("[" + topic + "] 已订阅，无需重复订阅");
        }
        return Result.data(doSubscribe(topic));
    }

    /**
     * 取消指定 topic 的订阅.
     * <p>
     * DELETE /pubsub/unsubscribe?topic=demo:event
     */
    @DeleteMapping("/unsubscribe")
    public Result<String> unsubscribe(@RequestParam String topic) {
        RedisPubSub.Subscription sub = subscriptions.remove(topic);
        if (sub == null) {
            return Result.data("[" + topic + "] 当前未订阅");
        }
        sub.unsubscribe();
        log.info("取消订阅 [{}]", topic);
        return Result.data("已取消订阅 [" + topic + "]");
    }

    /**
     * 取消所有订阅（走 Subscription 句柄逐个取消）.
     * <p>
     * POST /pubsub/unsubscribeAll
     */
    @PostMapping("/unsubscribeAll")
    public Result<String> unsubscribeAll() {
        unsubscribeAllInternal("manual");
        return Result.data("已取消所有订阅");
    }

    /**
     * 批量取消某 topic 的所有订阅（不依赖当前持有的 Subscription 句柄）.
     * <p>
     * 即使别的组件/实例也订阅了同一 topic，也会被一并取消。
     * <p>
     * POST /pubsub/unsubscribeTopic?topic=demo:event
     */
    @PostMapping("/unsubscribeTopic")
    public Result<String> unsubscribeTopic(@RequestParam String topic) {
        redisPubSub.unsubscribeTopic(topic);
        subscriptions.remove(topic);
        log.info("批量取消 topic [{}] 的所有订阅", topic);
        return Result.data("已批量取消 topic [" + topic + "] 的所有订阅");
    }

    // ==================== 收到的消息 ====================

    /**
     * 查看收到的最近消息列表.
     * <p>
     * GET /pubsub/received
     */
    @GetMapping("/received")
    public Result<List<ReceivedMessage>> received() {
        return Result.data(new ArrayList<>(received));
    }

    /**
     * 清空已收到的消息列表.
     * <p>
     * POST /pubsub/clear
     */
    @PostMapping("/clear")
    public Result<String> clear() {
        received.clear();
        return Result.data("已清空接收列表");
    }

    /**
     * 查看当前订阅状态.
     * <p>
     * GET /pubsub/status
     */
    @GetMapping("/status")
    public Result<Status> status() {
        Status s = new Status();
        s.setSubscribed(!subscriptions.isEmpty());
        s.setTopics(new TreeSet<>(subscriptions.keySet()));
        s.setReceivedCount(received.size());
        return Result.data(s);
    }

    // ==================== 内部 ====================

    /** 执行实际的订阅逻辑. */
    private String doSubscribe(String topic) {
        RedisPubSub.Subscription sub = redisPubSub.subscribe(topic, String.class, msg -> {
            ReceivedMessage rm = new ReceivedMessage(
                LocalTime.now().format(HMS), topic, msg);
            received.addFirst(rm);
            // 超过上限时丢弃最旧的
            while (received.size() > MAX_RETAINED) {
                received.pollLast();
            }
            log.info("[{}] 收到来自 [{}] 的消息: {}", rm.getReceivedAt(), topic, msg);
        });
        subscriptions.put(topic, sub);
        log.info("已订阅 [{}]，当前活跃订阅数: {}", topic, subscriptions.size());
        return "已订阅 [" + topic + "]，当前活跃订阅数: " + subscriptions.size();
    }

    /** 取消所有订阅并清理 map. */
    private void unsubscribeAllInternal(String reason) {
        subscriptions.forEach((topic, sub) -> {
            try {
                sub.unsubscribe();
            } catch (Exception e) {
                log.warn("取消订阅 [{}] 失败: {}", topic, e.getMessage());
            }
        });
        subscriptions.clear();
        log.info("已取消所有订阅 ({})", reason);
    }

    /** 收到的消息. */
    @Getter
    public static class ReceivedMessage {
        private final String receivedAt;
        private final String topic;
        private final String message;

        public ReceivedMessage(String receivedAt, String topic, String message) {
            this.receivedAt = receivedAt;
            this.topic = topic;
            this.message = message;
        }
    }

    /** 订阅状态. */
    @Data
    public static class Status {
        private boolean subscribed;
        private Set<String> topics;
        private int receivedCount;
    }
}