package com.aurora.example.controller;

import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.xlock.annotation.XKey;
import com.aurora.starter.xlock.annotation.XLock;
import com.aurora.starter.xlock.exception.LockException;
import com.aurora.starter.xlock.model.KeyInfo;
import com.aurora.starter.xlock.model.XLockType;
import com.aurora.starter.xlock.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式锁功能演示.
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/lock")
public class LockDemoController {

    @Autowired
    private LockService lockService;

    /**
     * 模拟共享资源（库存）.
     */
    private final AtomicInteger stock = new AtomicInteger(100);

    /**
     * 记录每个 worker 处理次数.
     */
    private final Map<String, Integer> workerCount = new ConcurrentHashMap<>();

    // ==================== 注解方式 ====================

    /**
     * 注解方式：可重入锁，扣减库存.
     * GET /lock/deduct?count=1
     *
     * 并发场景下同时扣减库存，不会超卖.
     */
    @XLock(prefix = "STOCK", keys = {"'DEDUCT'"}, waitTime = 5)
    @GetMapping("/deduct")
    public Result<Map<String, Object>> deduct(@RequestParam(defaultValue = "1") int count) {
        if (stock.get() < count) {
            return Result.error("库存不足，当前库存: " + stock.get());
        }
        int remaining = stock.addAndGet(-count);
        Map<String, Object> result = Map.of("deducted", count, "remaining", remaining);
        log.info("扣减库存成功: {}", result);
        return Result.data(result);
    }

    /**
     * 注解方式：多 key 组合锁定.
     * GET /lock/multi?userId=1&orderId=O001
     *
     * 同一 userId+orderId 的组合串行执行.
     */
    @XLock(prefix = "ORDER_PROCESS", keys = {"#userId", "#orderId"}, waitTime = 3)
    @GetMapping("/multi")
    public Result<String> multiLock(@RequestParam String userId, @RequestParam String orderId)
            throws InterruptedException {
        log.info("处理订单: userId={}, orderId={}", userId, orderId);
        Thread.sleep(1000); // 模拟业务处理
        return Result.data("订单处理完成: " + orderId);
    }

    /**
     * 注解方式：SpEL 表达式生成动态 key.
     * GET /lock/spel?name=zhangsan
     *
     * 演示 @XKey 参数级注解.
     */
    @XLock(prefix = "USER", keys = {})
    @GetMapping("/spel")
    public Result<String> spelLock(@XKey @RequestParam String name) {
        log.info("处理用户: {}", name);
        return Result.data("处理完成: " + name);
    }

    /**
     * 注解方式：公平锁.
     * GET /lock/fair?id=1
     */
    @XLock(prefix = "FAIR_TEST", keys = {"#id"}, lockType = XLockType.FAIR, waitTime = 10)
    @GetMapping("/fair")
    public Result<String> fairLock(@RequestParam String id) throws InterruptedException {
        Thread.sleep(2000); // 模拟耗时处理
        return Result.data("公平锁处理完成: " + id);
    }

    /**
     * 注解方式：自定义错误信息.
     * GET /lock/rush?skuId=1001
     *
     * 高并发抢购场景，拿不到锁返回自定义提示.
     */
    @XLock(prefix = "RUSH_BUY", keys = {"#skuId"}, waitTime = 2,
           errorMessage = "抢购人数过多，请稍后再试")
    @GetMapping("/rush")
    public Result<String> rushBuy(@RequestParam String skuId) throws InterruptedException {
        Thread.sleep(3000); // 模拟下单
        return Result.data("抢购成功: skuId=" + skuId);
    }

    // ==================== 编程方式 ====================

    /**
     * 编程方式：可重入锁.
     * GET /lock/manual?worker=A
     *
     * 多个 worker 并发调此接口，同一时间只有一个 worker 能执行.
     */
    @GetMapping("/manual")
    public Result<String> manualLock(@RequestParam(defaultValue = "default") String worker) {
        KeyInfo keyInfo = KeyInfo.builder()
                .prefix("MANUAL_TASK")
                .keys(new String[]{worker})
                .waitTime(3)
                .errorMessage("任务正在处理中，请稍后再试")
                .build();

        return Result.data(lockService.lock(keyInfo, () -> {
            workerCount.merge(worker, 1, Integer::sum);
            Thread.sleep(500); // 模拟处理
            return "Worker[" + worker + "] 处理完成, 累计处理次数: " + workerCount.get(worker);
        }));
    }

    /**
     * 编程方式：指定锁类型（公平锁）.
     * GET /lock/manual/fair?key=task1
     */
    @GetMapping("/manual/fair")
    public Result<String> manualFairLock(@RequestParam String key) {
        KeyInfo keyInfo = KeyInfo.builder()
                .prefix("FAIR_MANUAL")
                .keys(new String[]{key})
                .waitTime(5)
                .build();

        return Result.data(lockService.lock(keyInfo, XLockType.FAIR, () -> {
            Thread.sleep(1000);
            return "公平锁处理完成: " + key;
        }));
    }

    // ==================== 异常演示 ====================

    /**
     * 先占住锁不放.
     * GET /lock/hold?key=test
     *
     * 先调此接口占住锁（sleep 30s 不释放），
     * 再用另一个窗口调 /lock/tryAcquire?key=test 验证超时.
     */
    @XLock(prefix = "LOCK_TEST", keys = {"#key"})
    @GetMapping("/hold")
    public Result<String> holdLock(@RequestParam String key) throws InterruptedException {
        log.info("锁已被占住，30 秒后释放: key={}", key);
        Thread.sleep(30000);
        return Result.data("锁已释放: " + key);
    }

    /**
     * 尝试获取锁，1 秒超时.
     * GET /lock/tryAcquire?key=test
     *
     * 前提：另一个窗口正持有同一把锁（调用 /lock/hold?key=test），
     * 此接口会因 waitTime 超时而失败.
     */
    @GetMapping("/tryAcquire")
    public Result<String> tryAcquireLock(@RequestParam String key) {
        KeyInfo keyInfo = KeyInfo.builder()
                .prefix("LOCK_TEST")
                .keys(new String[]{key})
                .waitTime(1)
                .errorMessage("锁获取超时: " + key)
                .build();

        try {
            return Result.data(lockService.lock(keyInfo, () -> "获取锁成功: " + key));
        } catch (LockException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 查看当前库存.
     * GET /lock/stock
     */
    @GetMapping("/stock")
    public Result<Integer> getStock() {
        return Result.data(stock.get());
    }

    /**
     * 重置库存.
     * GET /lock/stock/reset
     */
    @GetMapping("/stock/reset")
    public Result<String> resetStock() {
        stock.set(100);
        return Result.data("库存已重置为 100");
    }
}
