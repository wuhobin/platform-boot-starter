package com.aurora.starter.xlock.lock;

import com.aurora.starter.xlock.exception.LockException;
import com.aurora.starter.xlock.model.KeyInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Objects;

/**
 * 锁服务有状态数据多线程处理.
 *
 * @author whb
 */
public abstract class BaseLock implements Lock {

    /**
     * redisson 客户端.
     */
    protected final RedissonClient redissonClient;

    protected BaseLock(final RedissonClient redissonClient) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient 不能为null");
    }

    @Override
    public boolean isLocked(final KeyInfo keyInfo) {
        Objects.requireNonNull(keyInfo, "keyInfo: 不能为null");
        return getLock(keyInfo).isLocked();
    }

    @Override
    public boolean tryLock(final KeyInfo keyInfo) {
        RLock lock = getLock(keyInfo);
        try {
            if (enableLeaseTime(keyInfo)) {
                if (enableWaitTime(keyInfo)) {
                    return lock.tryLock(keyInfo.getWaitTime(), keyInfo.getLeaseTime(), keyInfo.getTimeUnit());
                }
                return lock.tryLock(keyInfo.getLeaseTime(), keyInfo.getTimeUnit());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public void lock(final KeyInfo keyInfo) {
        Objects.requireNonNull(keyInfo, "keyInfo: 不能为null");
        if (keyInfo.isEmpty()) {
            throw new LockException("keyInfo keys不能为空");
        }

        RLock lock = getLock(keyInfo);
        if (enableWaitTime(keyInfo)) {
            try {
                if (!lock.tryLock(keyInfo.getWaitTime(), keyInfo.getLeaseTime(), keyInfo.getTimeUnit())) {
                    throw new LockException("获取锁失败：" + keyInfo.getKey());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockException("获取锁被中断：" + keyInfo.getKey(), e);
            }
        } else if (enableLeaseTime(keyInfo)) {
            lock.lock(keyInfo.getLeaseTime(), keyInfo.getTimeUnit());
        } else {
            lock.lock();
        }
    }

    @Override
    public void unlock(final KeyInfo keyInfo) {
        RLock lock = getLock(keyInfo);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 实现不同类型lock.
     *
     * @param keyInfo 锁的key信息
     * @return 锁对象
     */
    protected abstract RLock getLock(KeyInfo keyInfo);
}
