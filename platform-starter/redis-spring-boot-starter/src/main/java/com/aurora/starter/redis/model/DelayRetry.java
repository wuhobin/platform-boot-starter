package com.aurora.starter.redis.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 延迟重试数据.
 *
 * @author whb
 * @date 2026/06/18
 */
@Data
@Accessors(chain = true)
public class DelayRetry<T> implements Serializable {

    /**
     * 默认最大重试次数 5次
     */
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 默认每次重试时间间隔 15秒
     */
    private static final long DEFAULT_INTERVAL = 15L;

    /**
     * 当前重试次数
     */
    private Integer count = 0;

    /**
     * 最大重试次数
     */
    private Integer maxCount = MAX_RETRY_COUNT;

    /**
     * 每次重试时间间隔 单位:秒
     */
    private Long interval = DEFAULT_INTERVAL;

    /**
     * 是否使用相同的时间间隔 否的话会使用重试次数*间隔时间 默认false
     */
    private boolean useSameInterval;

    /**
     * 数据
     */
    private T data;

    /**
     * 下次重试时间间隔
     *
     * @return 次数 * 间隔
     */
    public long nextTime() {
        return useSameInterval ? interval : count * interval;
    }

    /**
     * 自增重试次数
     *
     * @return
     */
    public int addCount() {
        return count++;
    }

    /**
     * 自增重试次数并判断是否可继续重试.
     *
     * @return true 可继续重试
     */
    public boolean checkAndIncrement() {
        return addCount() < maxCount;
    }

}
