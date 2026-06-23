package com.aurora.example.controller.redis;

import com.aurora.starter.redis.core.RedisBloomFilter;
import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.redis.model.BloomFilterStats;
import com.aurora.starter.webmvc.domain.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 布隆过滤器功能演示.
 *
 * @author whb
 */
@Slf4j
@RestController
@RequestMapping("/bloom")
public class BloomFilterDemoController {

    @Autowired
    private Map<String, RedisBloomFilter<?>> bloomFilters;

    @Autowired
    private RedisCache redisCache;

    // ==================== 基础操作 ====================

    /**
     * 添加元素到布隆过滤器.
     * GET /bloom/add?filter=user:bloom&key=user:1001
     */
    @GetMapping("/add")
    public Result<Boolean> add(@RequestParam String filter,
                               @RequestParam String key) {
        RedisBloomFilter<String> bf = getFilter(filter);
        return Result.data(bf.add(key));
    }

    /**
     * 判断元素是否可能存在.
     * GET /bloom/contains?filter=user:bloom&key=user:1001
     */
    @GetMapping("/contains")
    public Result<Boolean> contains(@RequestParam String filter,
                                     @RequestParam String key) {
        RedisBloomFilter<String> bf = getFilter(filter);
        return Result.data(bf.contains(key));
    }

    /**
     * 获取近似元素数量.
     * GET /bloom/count?filter=user:bloom
     */
    @GetMapping("/count")
    public Result<Long> count(@RequestParam String filter) {
        RedisBloomFilter<String> bf = getFilter(filter);
        return Result.data(bf.count());
    }

    /**
     * 获取布隆过滤器统计信息.
     * GET /bloom/stats?filter=user:bloom
     */
    @GetMapping("/stats")
    public Result<BloomFilterStats> stats(@RequestParam String filter) {
        RedisBloomFilter<String> bf = getFilter(filter);
        return Result.data(bf.getStats());
    }

    /**
     * 删除布隆过滤器（不可逆）.
     * DELETE /bloom/delete?filter=user:bloom
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam String filter) {
        RedisBloomFilter<String> bf = getFilter(filter);
        return Result.data(bf.delete());
    }

    // ==================== 缓存穿透防护 ====================

    /**
     * 带布隆过滤器防护的缓存写入与读取.
     * POST /bloom/cache?filter=user:bloom&key=user:1001&value=zhangsan
     */
    @PostMapping("/cache")
    public Result<String> cacheOperation(@RequestParam String filter,
                                          @RequestParam String key,
                                          @RequestParam String value) {
        RedisBloomFilter<String> bf = getFilter(filter);
        // 写入缓存前先加入布隆过滤器
        bf.add(key);
        redisCache.setCacheObject(key, value);
        log.info("写入缓存: {} = {}", key, value);

        // 带布隆过滤器防护的读取
        String cached = redisCache.getCacheObject(key, bf);
        return Result.data("写入成功，读取结果: " + cached);
    }

    /**
     * 演示缓存穿透防护：查询不存在的 key.
     * GET /bloom/miss?filter=user:bloom&key=user:99999
     */
    @GetMapping("/miss")
    public Result<String> cacheMiss(@RequestParam String filter,
                                     @RequestParam String key) {
        RedisBloomFilter<String> bf = getFilter(filter);
        // 布隆过滤器判定不存在 → 直接返回 null，不查 Redis
        String value = redisCache.getCacheObject(key, bf);
        if (value == null) {
            return Result.data("布隆过滤器命中：key [" + key + "] 不存在，跳过 Redis 查询");
        }
        return Result.data(value);
    }

    /**
     * protect 方法演示：布隆过滤器 + 自定义数据源.
     * GET /bloom/protect?filter=user:bloom&key=user:1001
     */
    @GetMapping("/protect")
    public Result<String> protect(@RequestParam String filter,
                                   @RequestParam String key) {
        RedisBloomFilter<String> bf = getFilter(filter);
        String result = bf.protect(key, () -> {
            log.info("布隆过滤器判定可能存在，执行数据加载...");
            // 模拟从 Redis / DB 加载数据
            return (String) redisCache.getCacheObject(key);
        });
        return Result.data(result);
    }

    @SuppressWarnings("unchecked")
    private RedisBloomFilter<String> getFilter(String name) {
        RedisBloomFilter<String> bf = (RedisBloomFilter<String>) bloomFilters.get(name);
        if (bf == null) {
            throw new IllegalArgumentException("布隆过滤器 [" + name + "] 不存在，可用: " + bloomFilters.keySet());
        }
        return bf;
    }
}
