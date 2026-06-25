package com.aurora.example.controller.redis;

import com.aurora.starter.redis.core.TwoLevelCache;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import com.aurora.starter.webmvc.domain.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 两级缓存功能演示.
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/two-level")
public class TwoLevelCacheDemoController {

    @Autowired
    private TwoLevelCacheManager cacheManager;

    /** 模拟数据库 */
    private final Map<String, String> mockDb = new ConcurrentHashMap<>();
    private final AtomicInteger dbHitCounter = new AtomicInteger(0);

    public TwoLevelCacheDemoController() {
        mockDb.put("user:1001", "张三");
        mockDb.put("user:1002", "李四");
        mockDb.put("product:001", "iPhone 15");
        mockDb.put("product:002", "MacBook Pro");
    }

    // ==================== 实例信息 ====================

    /**
     * 查看所有缓存实例.
     * GET /two-level/instances
     */
    @GetMapping("/instances")
    public Result<Set<String>> instances() {
        return Result.data(cacheManager.names());
    }

    // ==================== 纯读（不回源） ====================

    /**
     * 纯读：L1 → L2 穿透，不回源查 DB.
     * GET /two-level/get?cache=default&key=user:1001
     */
    @GetMapping("/get")
    public Result<String> get(@RequestParam(defaultValue = "default") String cache,
                               @RequestParam String key) {
        TwoLevelCache c = cacheManager.get(cache);
        String value = c.get(key);
        if (value == null) {
            return Result.data("缓存未命中: " + key);
        }
        return Result.data(value);
    }

    // ==================== 带回源（防击穿） ====================

    /**
     * 带回源读取：L1 → L2 → mock DB.
     * GET /two-level/getWithLoader?cache=default&key=user:1001
     */
    @GetMapping("/getWithLoader")
    public Result<String> getWithLoader(@RequestParam(defaultValue = "default") String cache,
                                         @RequestParam String key) {
        TwoLevelCache c = cacheManager.get(cache);
        String value = c.get(key, () -> {
            int hit = dbHitCounter.incrementAndGet();
            log.info("[{}] 缓存未命中，回源查 DB (第 {} 次)", LocalTime.now(), hit);
            return mockDb.get(key);
        });
        if (value == null) {
            return Result.data("数据不存在: " + key + " (已缓存空值，1分钟内不会再次穿透)");
        }
        return Result.data(value + " (DB 命中次数: " + dbHitCounter.get() + ")");
    }

    /**
     * 带回源 + 自定义 TTL.
     * GET /two-level/getWithTtl?cache=default&key=user:1001&ttl=10
     */
    @GetMapping("/getWithTtl")
    public Result<String> getWithTtl(@RequestParam(defaultValue = "default") String cache,
                                      @RequestParam String key,
                                      @RequestParam long ttl) {
        TwoLevelCache c = cacheManager.get(cache);
        String value = c.get(key, () -> {
            log.info("[{}] 回源查 DB, TTL={}s", LocalTime.now(), ttl);
            return mockDb.get(key);
        }, ttl, TimeUnit.SECONDS);
        return Result.data(value);
    }

    // ==================== 写入 ====================

    /**
     * 写入缓存（写穿 L1 + L2）.
     * POST /two-level/set?cache=default&key=user:1003&value=王五&ttl=120
     */
    @PostMapping("/set")
    public Result<String> set(@RequestParam(defaultValue = "default") String cache,
                               @RequestParam String key,
                               @RequestParam String value,
                               @RequestParam(defaultValue = "60") long ttl) {
        TwoLevelCache c = cacheManager.get(cache);
        c.set(key, value, ttl, TimeUnit.SECONDS);
        mockDb.put(key, value);
        log.info("[{}] 写入缓存: {} = {} (TTL={}s)", LocalTime.now(), key, value, ttl);
        return Result.data("写入成功: " + key + "=" + value + ", TTL=" + ttl + "s");
    }

    // ==================== 删除 ====================

    /**
     * 删除缓存（L1 + L2 + 广播）.
     * DELETE /two-level/evict?cache=default&key=user:1001
     */
    @DeleteMapping("/evict")
    public Result<String> evict(@RequestParam(defaultValue = "default") String cache,
                                 @RequestParam String key) {
        TwoLevelCache c = cacheManager.get(cache);
        c.evict(key);
        log.info("[{}] 删除缓存: {}", LocalTime.now(), key);
        return Result.data("已删除: " + key);
    }

    // ==================== 清空本地 ====================

    /**
     * 清空本地 L1 缓存（不碰 Redis）.
     * POST /two-level/clearLocal?cache=default
     */
    @PostMapping("/clearLocal")
    public Result<String> clearLocal(@RequestParam(defaultValue = "default") String cache) {
        TwoLevelCache c = cacheManager.get(cache);
        c.clearLocal();
        log.info("[{}] 清空本地 L1", LocalTime.now());
        return Result.data("本地 L1 已清空");
    }

    // ==================== 防穿透演示 ====================

    /**
     * 演示空值防穿透：查询 DB 中不存在的 key.
     * GET /two-level/nullPenetration?cache=default&key=user:unknown
     *
     * 首次：回源 → null → 缓存空值 60s
     * 60s 内重复请求：L1 命中空值 → 直接返回 null，不走 DB
     */
    @GetMapping("/nullPenetration")
    public Result<String> nullPenetration(@RequestParam(defaultValue = "default") String cache,
                                           @RequestParam String key) {
        TwoLevelCache c = cacheManager.get(cache);
        String value = c.get(key, () -> {
            int hit = dbHitCounter.incrementAndGet();
            log.info("[{}] 穿透！回源查 DB (第 {} 次)", LocalTime.now(), hit);
            return mockDb.get(key);  // 不存在的 key 返回 null
        });
        return Result.data(value != null ? value
                : "key [" + key + "] 不存在 (DB 总命中: " + dbHitCounter.get() + ")");
    }

    // ==================== DB 计数器 ====================

    /**
     * 查看 DB 命中次数.
     * GET /two-level/dbHits
     */
    @GetMapping("/dbHits")
    public Result<Integer> dbHits() {
        return Result.data(dbHitCounter.get());
    }

    /**
     * 重置 DB 计数器.
     * POST /two-level/reset
     */
    @PostMapping("/reset")
    public Result<String> reset() {
        dbHitCounter.set(0);
        return Result.data("DB 计数器已重置");
    }
}
