package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;

/**
 * 写锁操作服务.
 *
 * @author breggor
 */
public class WriteLock extends BaseLock implements Lock {

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return getRedissonClient().getReadWriteLock(keyInfo.getKey()).writeLock();
    }
}
