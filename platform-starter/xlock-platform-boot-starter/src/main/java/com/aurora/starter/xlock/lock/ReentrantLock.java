package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;

/**
 * 可重入锁加锁服务.
 *
 * @author whb
 */
public class ReentrantLock extends BaseLock implements Lock {

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return getRedissonClient().getLock(keyInfo.getKey());
    }
}
