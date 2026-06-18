package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;

import java.util.List;

/**
 * 联锁操作服务.
 *
 * @author whb
 */
public class MultiLock extends BaseLock implements Lock {

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
         List<String> keys = keyInfo.getRealKeys();
        RLock[] lockList = new RLock[keys.size()];
        for (int i = 0, size = keys.size(); i < size; i++) {
            lockList[i] = getRedissonClient().getLock(keys.get(i));
        }
        return new RedissonMultiLock(lockList);
    }

    @Override
    public void unlock(final KeyInfo keyInfo) {
        RLock lock = getLock(keyInfo);
        lock.unlock();
    }
}
