package com.aurora.starter.xlock.lock;


import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;

/**
 * 公平锁操作服务.
 *
 * @author whb
 */
public class FairLock extends BaseLock implements Lock {

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return getRedissonClient().getFairLock(keyInfo.getKey());
    }
}
