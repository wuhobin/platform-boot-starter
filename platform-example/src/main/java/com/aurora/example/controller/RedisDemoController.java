package com.aurora.example.controller;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.webmvc.domain.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存功能演示.
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisDemoController {

    @Autowired
    private RedisCache redisCache;

    // ==================== String 操作 ====================

    /**
     * 写入字符串缓存并设置过期时间.
     * GET /redis/set?key=name&value=zhangsan&ttl=60
     */
    @GetMapping("/set")
    public Result<String> set(@RequestParam String key,
                              @RequestParam String value,
                              @RequestParam(defaultValue = "60") long ttl) {
        redisCache.setCacheObject(key, value, ttl, TimeUnit.SECONDS);
        return Result.data("OK: " + key + "=" + value + ", ttl=" + ttl + "s");
    }

    /**
     * 读取字符串缓存.
     * GET /redis/get?key=name
     */
    @GetMapping("/get")
    public Result<Object> get(@RequestParam String key) {
        Object value = redisCache.getCacheObject(key);
        return Result.data(value);
    }

    /**
     * 判断 key 是否存在.
     * GET /redis/exists?key=name
     */
    @GetMapping("/exists")
    public Result<Boolean> exists(@RequestParam String key) {
        return Result.data(redisCache.exists(key));
    }

    /**
     * 删除 key.
     * GET /redis/delete?key=name
     */
    @GetMapping("/delete")
    public Result<Boolean> delete(@RequestParam String key) {
        return Result.data(redisCache.deleteObject(key));
    }

    /**
     * setIfAbsent 演示（分布式锁场景）.
     * GET /redis/setIfAbsent?key=lock:order&value=locked
     */
    @GetMapping("/setIfAbsent")
    public Result<Boolean> setIfAbsent(@RequestParam String key,
                                       @RequestParam String value) {
        return Result.data(redisCache.setIfAbsent(key, value, 10, TimeUnit.SECONDS));
    }

    // ==================== 自增自减 ====================

    /**
     * 自增计数器.
     * GET /redis/incr?key=counter
     */
    @GetMapping("/incr")
    public Result<Long> increment(@RequestParam String key) {
        return Result.data(redisCache.increment(key));
    }

    /**
     * 自减计数器.
     * GET /redis/decr?key=counter
     */
    @GetMapping("/decr")
    public Result<Long> decrement(@RequestParam String key) {
        return Result.data(redisCache.decrement(key));
    }

    // ==================== List 操作 ====================

    /**
     * 往 List 尾部添加元素.
     * GET /redis/list/add?key=tasks&value=task1
     */
    @GetMapping("/list/add")
    public Result<Long> listAdd(@RequestParam String key, @RequestParam String value) {
        return Result.data(redisCache.addCacheList(key, value));
    }

    /**
     * 获取整个 List.
     * GET /redis/list/get?key=tasks
     */
    @GetMapping("/list/get")
    public Result<List<Object>> listGet(@RequestParam String key) {
        return Result.data(redisCache.getCacheList(key));
    }

    // ==================== Set 操作 ====================

    /**
     * 往 Set 添加元素.
     * GET /redis/set/add?key=tags&value=java
     */
    @GetMapping("/set/add")
    public Result<Long> setAdd(@RequestParam String key, @RequestParam String value) {
        return Result.data(redisCache.addCacheSet(key, value));
    }

    /**
     * 获取整个 Set.
     * GET /redis/set/get?key=tags
     */
    @GetMapping("/set/get")
    public Result<Set<Object>> setGet(@RequestParam String key) {
        Set<Object> cacheSet = redisCache.getCacheSet(key);
        return Result.data(cacheSet);
    }

    // ==================== Hash 操作 ====================

    /**
     * Hash 设置单个字段.
     * GET /redis/hash/set?key=user:1&field=name&value=zhangsan
     */
    @GetMapping("/hash/set")
    public Result<String> hashSet(@RequestParam String key,
                                  @RequestParam String field,
                                  @RequestParam String value) {
        redisCache.setCacheMapValue(key, field, value);
        return Result.data("OK");
    }

    /**
     * Hash 获取单个字段.
     * GET /redis/hash/get?key=user:1&field=name
     */
    @GetMapping("/hash/get")
    public Result<Object> hashGet(@RequestParam String key, @RequestParam String field) {
        return Result.data(redisCache.getCacheMapValue(key, field));
    }

    /**
     * Hash 获取所有字段.
     * GET /redis/hash/getAll?key=user:1
     */
    @GetMapping("/hash/getAll")
    public Result<Map<String, Object>> hashGetAll(@RequestParam String key) {
        return Result.data(redisCache.getCacheMap(key));
    }

    // ==================== ZSet 操作 ====================

    /**
     * ZSet 添加元素（模拟排行榜）.
     * GET /redis/zset/add?key=rank&member=player1&score=980
     */
    @GetMapping("/zset/add")
    public Result<Boolean> zsetAdd(@RequestParam String key,
                                   @RequestParam String member,
                                   @RequestParam double score) {
        return Result.data(redisCache.addZset(key, member, score));
    }

    /**
     * 获取 ZSet 排行榜（正序）.
     * GET /redis/zset/rank?key=rank
     */
    @GetMapping("/zset/rank")
    public Result<Set<Object>> zsetRank(@RequestParam String key) {
        return Result.data(redisCache.getCacheZSet(key));
    }

    /**
     * 获取 ZSet 排行榜（倒序）.
     * GET /redis/zset/reverseRank?key=rank
     */
    @GetMapping("/zset/reverseRank")
    public Result<Set<Object>> zsetReverseRank(@RequestParam String key) {
        return Result.data(redisCache.getCacheReverseZSet(key));
    }

    // ==================== 唯一 ID 生成 ====================

    /**
     * 生成全局唯一 ID.
     * GET /redis/soleId?prefix=ORDER
     * 返回示例: ORDER202606180001
     */
    @GetMapping("/soleId")
    public Result<String> soleId(@RequestParam(defaultValue = "ORDER") String prefix) {
        return Result.data(redisCache.soleId(prefix, "", "seq:" + prefix, new Date()));
    }

    // ==================== 扫描操作 ====================

    /**
     * SCAN 扫描匹配的 key.
     * GET /redis/scan?pattern=user:*
     */
    @GetMapping("/scan")
    public Result<Collection<String>> scan(@RequestParam(defaultValue = "*") String pattern) {
        return Result.data(redisCache.scan(pattern));
    }
}
