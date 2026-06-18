package com.aurora.starter.xlock.lock;


import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 读锁操作服务.
 *
 * @author breggor
 */
public class ReadLock extends BaseLock implements Lock {

    public ReadLock(final RedissonClient redissonClient) {
        super(redissonClient);
    }

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return redissonClient.getReadWriteLock(keyInfo.getKey()).readLock();
    }
}
