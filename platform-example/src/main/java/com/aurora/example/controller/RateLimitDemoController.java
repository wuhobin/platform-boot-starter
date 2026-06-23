package com.aurora.example.controller;

import com.aurora.starter.redis.core.RedisRateLimiter;
import com.aurora.starter.redis.exception.RateLimiterException;
import com.aurora.starter.webmvc.domain.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流功能演示.
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/rate-limit")
public class RateLimitDemoController {

    @Autowired
    private RedisRateLimiter rateLimiter;

    private final AtomicInteger counter = new AtomicInteger(0);

    // ==================== 阻塞等待模式 ====================

    /**
     * 阻塞等待限流：每分钟 3 次.
     * GET /rate-limit/block?key=test
     *
     * 快速连续请求 4 次，前 3 次立刻返回，第 4 次会阻塞等待.
     */
    @GetMapping("/block")
    public Result<String> block(@RequestParam(defaultValue = "test") String key) {
        int seq = counter.incrementAndGet();
        log.info("[{}] 第 {} 次请求到达", LocalTime.now(), seq);

        rateLimiter.rateLimit("RATE_DEMO:" + key, 3, 60, () -> {
            log.info("[{}] 第 {} 次执行业务", LocalTime.now(), seq);
        });

        return Result.data("第 " + seq + " 次请求通过");
    }

    // ==================== 尝试获取（非阻塞） ====================

    /**
     * 非阻塞限流：每秒 2 次，不等待.
     * GET /rate-limit/try?key=test
     *
     * 快速连续请求，超限的直接返回 false.
     */
    @GetMapping("/try")
    public Result<String> tryAcquire(@RequestParam(defaultValue = "test") String key) {
        boolean acquired = rateLimiter.tryRateLimit("RATE_DEMO_TRY:" + key, 2, 1, () -> {
            log.info("[{}] 执行中...", LocalTime.now());
        });

        return acquired
                ? Result.data("请求已处理")
                : Result.error(429, "当前请求过多，请稍后重试");
    }

    // ==================== 尝试获取（带等待） ====================

    /**
     * 带等待时间的限流：每秒 2 次，最多等 3 秒.
     * GET /rate-limit/tryWait?key=test
     */
    @GetMapping("/tryWait")
    public Result<String> tryWait(@RequestParam(defaultValue = "test") String key) {
        boolean acquired = rateLimiter.tryRateLimit("RATE_DEMO_WAIT:" + key, 2, 1, 3, () -> {
            log.info("[{}] 等待后执行", LocalTime.now());
        });

        return acquired
                ? Result.data("请求已处理")
                : Result.error(429, "等待超时，请稍后重试");
    }

    // ==================== 有返回值的限流 ====================

    /**
     * 有返回值的限流：每分钟 5 次.
     * GET /rate-limit/supplier?key=test
     */
    @GetMapping("/supplier")
    public Result<Integer> supplier(@RequestParam(defaultValue = "test") String key) {
        try {
            Integer result = rateLimiter.tryRateLimit("RATE_DEMO_SUP:" + key, 5, 60, 2, () -> {
                log.info("[{}] 处理业务逻辑", LocalTime.now());
                return (int) (Math.random() * 100);
            });
            return Result.data(result);
        } catch (RateLimiterException e) {
            return Result.error(429, "限流中：" + e.getMessage());
        }
    }

    // ==================== 纯检查模式 ====================

    /**
     * 纯检查：只判断是否在限流窗口内，不执行业务.
     * GET /rate-limit/check?key=test
     */
    @GetMapping("/check")
    public Result<String> check(@RequestParam(defaultValue = "test") String key) {
        if (rateLimiter.tryAcquire("RATE_DEMO_CHECK:" + key)) {
            return Result.data("未触发限流");
        }
        return Result.error(429, "触发限流");
    }

    // ==================== 重置计数器 ====================

    /**
     * 重置请求计数.
     * POST /rate-limit/reset
     */
    @PostMapping("/reset")
    public Result<String> reset() {
        counter.set(0);
        return Result.data("计数器已重置");
    }
}
