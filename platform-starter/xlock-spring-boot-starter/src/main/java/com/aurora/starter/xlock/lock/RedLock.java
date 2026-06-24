package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;

/**
 * 红锁操作服务.
 *
 * @author breggor
 */
public class RedLock extends BaseLock implements Lock {

    public RedLock(final RedissonClient redissonClient) {
        super(redissonClient);
    }

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        List<String> keys = keyInfo.getRealKeys();
        RLock[] lockList = new RLock[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            lockList[i] = redissonClient.getLock(keys.get(i));
        }
        return new RedissonRedLock(lockList);
    }

    @Override
    public void unlock(final KeyInfo keyInfo) {
        RLock lock = getLock(keyInfo);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
