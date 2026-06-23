package com.aurora.starter.redis.model;

import java.io.Serializable;

/**
 * 布隆过滤器统计信息.
 *
 * @author whb
 */
public record BloomFilterStats(
        long count,
        long expectedInsertions,
        double falseProbability,
        int hashIterations,
        long size) implements Serializable {
}
