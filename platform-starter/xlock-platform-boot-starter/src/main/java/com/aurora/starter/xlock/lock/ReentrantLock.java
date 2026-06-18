package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 可重入锁加锁服务.
 *
 * @author whb
 */
public class ReentrantLock extends BaseLock implements Lock {

    public ReentrantLock(final RedissonClient redissonClient) {
        super(redissonClient);
    }

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return redissonClient.getLock(keyInfo.getKey());
    }
}
