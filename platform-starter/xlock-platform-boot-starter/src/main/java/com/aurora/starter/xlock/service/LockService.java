package com.aurora.starter.xlock.service;


import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.xlock.exception.LockException;
import com.aurora.starter.xlock.lock.Lock;
import com.aurora.starter.xlock.model.KeyInfo;
import com.aurora.starter.xlock.model.XLockType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Callable;

/**
 * 锁服务，用于非AOP锁使用.
 *
 * @author zenghao
 * @date 2024/5/30
 */
@Slf4j
public class LockService {

    @Autowired
    private LockFactory lockFactory;

    /**
     * 对supplier方法加可重入锁
     * @param keyInfo 锁信息
     * @param callable 方法
     * @return 正常执行返回supplier方法的响应值，加锁失败则抛出锁异常
     * @param <T> 响应结果
     */
    public <T> T lock(KeyInfo keyInfo, Callable<T> callable) {
        return lock(keyInfo, XLockType.REENTRANT, callable);
    }

    /**
     * 对supplier方法加锁
     * @param keyInfo 锁信息
     * @param lockType 锁类型
     * @param callable 方法
     * @return 正常执行返回supplier方法的响应值，加锁失败则抛出锁异常
     * @param <T> 响应结果
     */
    public <T> T lock(KeyInfo keyInfo, XLockType lockType, Callable<T> callable) {
        Lock lockImpl = lockFactory.getService(lockType);
        try {
            lockImpl.lock(keyInfo);

            T result = callable.call();
            if (!keyInfo.isDisableLog()) {
                log.info("[分布式锁] - 处理完：锁释放[{}]。返回值:{}", keyInfo, result);
            }
            return result;
        } catch (RuntimeException ex) {
            log.warn("[分布式锁] - 运行异常：导致锁释放[{}]", keyInfo, ex);
            throw ex;
        } catch (Exception ex) {
            log.warn("[分布式锁] - 内部异常：导致锁释放[{}]", keyInfo, ex);
            throw new LockException(StringUtils.defaultIfBlank(keyInfo.getErrorMessage(), "操作频繁，请稍后再试！"), ex);
        } finally {
            lockImpl.unlock(keyInfo);
        }
    }


}
