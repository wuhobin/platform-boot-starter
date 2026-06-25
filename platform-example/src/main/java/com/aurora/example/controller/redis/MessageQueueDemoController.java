package com.aurora.example.controller.redis;

import com.aurora.starter.redis.core.RedisMessageQueue;
import com.aurora.starter.redis.model.Message;
import com.aurora.starter.webmvc.domain.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列功能演示.
 * <p>
 * 基于 {@link RedisMessageQueue}，按 group 隔离的可靠消息队列。
 * <p>
 * <b>注意：</b>{@code poll} 会阻塞当前线程，本 Controller 在 HTTP 请求线程上调用，
 * 因此默认 timeout 设为 2 秒（避免长时间占用 Tomcat 工作线程）；需要更长 timeout 请走专用 worker。
 *
 * <pre>
 *   1. POST /mq/send?group=ORDER_EVENT&data=hello          # 生产消息
 *   2. GET  /mq/poll?group=ORDER_EVENT&timeout=2           # 消费（最多等 2 秒）
 *   3. POST /mq/sendAndReceive?group=ORDER_EVENT&data=hi   # 生产 + 立刻消费
 * </pre>
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/mq")
public class MessageQueueDemoController {

    /** HTTP 请求线程上的默认 poll 超时（秒），避免长时间阻塞. */
    private static final long DEFAULT_HTTP_TIMEOUT_SECONDS = 2L;

    @Autowired
    private RedisMessageQueue messageQueue;

    // ==================== 生产 ====================

    /**
     * 发送消息到指定 group；msgId 不传则自动生成 UUID.
     * <p>
     * POST /mq/send?group=ORDER_EVENT&data=hello&msgId=optional-id
     */
    @PostMapping("/send")
    public Result<String> send(@RequestParam(defaultValue = "ORDER_EVENT") String group,
                                @RequestParam String data,
                                @RequestParam(required = false) String msgId) {
        Message<String> msg = new Message<>();
        msg.setMsgId(msgId != null ? msgId : UUID.randomUUID().toString());
        msg.setData(data);
        messageQueue.send(group, msg);
        log.info("[{}] 生产消息: msgId={}, data={}", group, msg.getMsgId(), data);
        return Result.data("已发送到 [" + group + "]: msgId=" + msg.getMsgId());
    }

    // ==================== 消费 ====================

    /**
     * 阻塞拉取一条消息；timeout 默认 2 秒，最大 5 秒（避免长占请求线程）.
     * <p>
     * GET /mq/poll?group=ORDER_EVENT&timeout=2
     *
     * @return 拉取到的消息；超时或异常返回 null
     */
    @GetMapping("/poll")
    public Result<Message<?>> poll(@RequestParam(defaultValue = "ORDER_EVENT") String group,
                                    @RequestParam(defaultValue = "2") long timeout) {
        long bounded = Math.max(1, Math.min(timeout, 5));
        Message<?> message = messageQueue.poll(group, bounded, TimeUnit.SECONDS);
        if (message == null) {
            log.info("[{}] 拉取消息超时（{} 秒内无消息）", group, bounded);
            return Result.success("timeout", null);
        }
        log.info("[{}] 消费到消息: msgId={}, data={}", group, message.getMsgId(), message.getData());
        return Result.data(message);
    }

    // ==================== 复合：发完立刻收 ====================

    /**
     * 演示完整链路：发送一条消息后立刻尝试拉取（用独立 worker 线程，避免阻塞请求线程）.
     * <p>
     * POST /mq/sendAndReceive?group=ORDER_EVENT&data=hello&timeout=2
     *
     * @implNote 拉取动作在 worker 线程上执行；发送在调用线程上立即返回。
     */
    @PostMapping("/sendAndReceive")
    public Result<String> sendAndReceive(@RequestParam(defaultValue = "ORDER_EVENT") String group,
                                           @RequestParam String data,
                                           @RequestParam(defaultValue = "2") long timeout) throws Exception {
        Message<String> msg = new Message<>();
        msg.setMsgId(UUID.randomUUID().toString());
        msg.setData(data);
        messageQueue.send(group, msg);
        log.info("[{}] 生产消息: msgId={}", group, msg.getMsgId());

        // 把阻塞 poll 放到 worker 线程，主线程立刻返回"已发送"
        java.util.concurrent.FutureTask<Message<?>> task = new java.util.concurrent.FutureTask<>(
            () -> messageQueue.poll(group, Math.max(1, Math.min(timeout, 5)), TimeUnit.SECONDS));
        new Thread(task, "mq-demo-poll").start();
        Message<?> received = task.get(Math.max(1, Math.min(timeout + 1, 6)), TimeUnit.SECONDS);

        if (received == null) {
            return Result.data("已发送 msgId=" + msg.getMsgId() + "，但 " + timeout + " 秒内未消费到（队列可能堆积了别的消息）");
        }
        return Result.data("已发送并消费: msgId=" + msg.getMsgId()
                + " → 收到 msgId=" + received.getMsgId()
                + " data=" + received.getData());
    }
}