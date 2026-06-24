package com.aurora.starter.xlock.lock;


import com.aurora.starter.xlock.model.KeyInfo;

/**
 * 锁服务.
 *
 * @author whb
 */
public interface Lock {

    /**
     * 判断锁是否已存在.
     *
     * @param keyInfo 锁key信息
     * @return boolean
     */
    boolean isLocked(KeyInfo keyInfo);

    /**
     * 尝试加锁.
     *
     * @param keyInfo 锁key信息
     * @return true 加锁成功。false 加锁失败
     */
    boolean tryLock(KeyInfo keyInfo);

    /**
     * 加锁.
     *
     * @param keyInfo 锁key信息
     */
    void lock(KeyInfo keyInfo);

    /**
     * 解锁.
     *
     * @param keyInfo 锁key信息
     */
    void unlock(KeyInfo keyInfo);

    /**
     * 是否启用离开时间.
     *
     * @param keyInfo 锁key信息
     * @return true:启用 false:禁用
     */
    default boolean enableLeaseTime(final KeyInfo keyInfo) {
        return keyInfo.getLeaseTime() != -1;
    }

    /**
     * 是否启用等待时间.
     *
     * @param keyInfo 锁key信息
     * @return true:启用 false:禁用
     */
    default boolean enableWaitTime(KeyInfo keyInfo) {
        return keyInfo.getWaitTime() != -1;
    }
}
