package com.aurora.starter.redis.model;

import java.io.Serializable;

/**
 * 布隆过滤器统计信息.
 *
 * @author whb
 */
public record BloomFilterStats(
        /** 已添加元素数量 */
        long elementCount,
        /** 预期插入元素数量 */
        long expectedInsertions,
        /** 误判率（false positive probability） */
        double falsePositiveProbability,
        /** Hash 函数迭代次数 */
        int hashIterations,
        /** 位数组大小（bits） */
        long bitSize) implements Serializable {
}
