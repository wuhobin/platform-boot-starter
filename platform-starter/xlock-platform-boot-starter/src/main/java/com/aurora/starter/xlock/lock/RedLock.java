package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;

import java.util.List;

/**
 * 红锁操作服务.
 *
 * @author breggor
 */
public class RedLock extends BaseLock implements Lock {

    @Override
    public RLock getLock(final KeyInfo keyInfo) {
        List<String> keys = keyInfo.getRealKeys();
        RLock[] lockList = new RLock[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            lockList[i] = getRedissonClient().getLock(keys.get(i));
        }
        return new RedissonRedLock(lockList);
    }
}
