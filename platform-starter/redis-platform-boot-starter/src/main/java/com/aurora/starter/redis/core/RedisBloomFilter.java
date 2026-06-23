package com.aurora.starter.redis.core;

import com.aurora.starter.redis.model.BloomFilterStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;

import java.util.function.Supplier;

/**
 * Redis 布隆过滤器封装.
 * <p>
 * 基于 Redisson RBloomFilter，提供 add / contains / count / delete / stats 操作，
 * 以及缓存穿透防护的 protect 方法。
 *
 * @param <T> 元素类型
 * @author whb
 */
@Slf4j
public class RedisBloomFilter<T> {

    private final RBloomFilter<T> bloomFilter;

    @Getter
    private final String name;

    @Getter
    private final long expectedInsertions;

    @Getter
    private final double falsePositiveProbability;

    public RedisBloomFilter(RBloomFilter<T> bloomFilter, String name,
                            long expectedInsertions, double falsePositiveProbability) {
        this.bloomFilter = bloomFilter;
        this.name = name;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;
    }

    /**
     * 添加元素.
     *
     * @param value 元素值
     * @return true: 实际添加了元素（count 增加）, false: 元素已存在
     */
    public boolean add(T value) {
        return bloomFilter.add(value);
    }

    /**
     * 判断元素是否可能存在.
     * <p>
     * 返回 false 表示元素一定不存在；返回 true 表示元素可能存在（存在误判可能）。
     *
     * @param value 元素值
     * @return true: 可能存在, false: 一定不存在
     */
    public boolean contains(T value) {
        return bloomFilter.contains(value);
    }

    /**
     * 获取已添加元素近似数量.
     *
     * @return 元素数量
     */
    public long count() {
        return bloomFilter.count();
    }

    /**
     * 删除整个布隆过滤器（不可逆操作）.
     *
     * @return true: 删除成功
     */
    public boolean delete() {
        return bloomFilter.delete();
    }

    /**
     * 获取布隆过滤器统计信息.
     *
     * @return BloomFilterStats
     */
    public BloomFilterStats getStats() {
        return new BloomFilterStats(
                bloomFilter.count(),
                bloomFilter.getExpectedInsertions(),
                bloomFilter.getFalseProbability(),
                bloomFilter.getHashIterations(),
                bloomFilter.getSize()
        );
    }

    /**
     * 缓存穿透防护：先检查布隆过滤器，不存在则直接返回 null，避免查询缓存/数据库.
     * <p>
     * 如果布隆过滤器误判（contains 返回 true 但 loader 返回 null），会打印 warn 日志。
     *
     * @param cacheKey 缓存 key（同时作为布隆过滤器的检查值）
     * @param loader   数据加载器（如查询 Redis、数据库）
     * @param <R>      返回值类型
     * @return 数据，不存在返回 null
     */
    public <R> R protect(String cacheKey, Supplier<R> loader) {
        if (!bloomFilter.contains((T) cacheKey)) {
            return null;
        }
        R result = loader.get();
        if (result == null) {
            log.warn("Bloom filter [{}] false positive for key [{}]", name, cacheKey);
        }
        return result;
    }
}
