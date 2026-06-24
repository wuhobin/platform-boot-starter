package com.aurora.starter.xlock.lock;


import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 公平锁操作服务.
 *
 * @author whb
 */
public class FairLock extends BaseLock implements Lock {

    public FairLock(final RedissonClient redissonClient) {
        super(redissonClient);
    }

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        return redissonClient.getFairLock(keyInfo.getKey());
    }
}
