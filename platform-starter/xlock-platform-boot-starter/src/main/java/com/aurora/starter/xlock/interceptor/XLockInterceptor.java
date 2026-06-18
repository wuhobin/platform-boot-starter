package com.aurora.starter.xlock.interceptor;


import com.aurora.starter.common.utils.bean.ReflectUtils;
import com.aurora.starter.xlock.annotation.XLock;
import com.aurora.starter.xlock.model.KeyInfo;
import com.aurora.starter.xlock.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

/**
 * 锁拦截器.
 *
 * @author breggor
 */
@Slf4j
@Aspect
@Order(-1)
public class XLockInterceptor {

    @Autowired
    private LockService lockService;

    @Autowired
    private XLockSpelResolver xLockSpelResolver;

    /**
     * 方法加锁.
     *
     * @param joinPoint 切面方法
     * @param xLock     锁注解
     * @return 方法返回值
     */
    @Around(value = "@annotation(xLock)")
    public Object around(final ProceedingJoinPoint joinPoint, final XLock xLock) {
        String[] args = Arrays.stream(joinPoint.getArgs()).map(ReflectUtils::processObject).toArray(String[]::new);
        if (!xLock.disableLog()) {
            log.info("[分布式锁] - 拦截进入处理：{} {}", joinPoint, args);
        }
        KeyInfo keyInfo = xLockSpelResolver.getKeyInfo(joinPoint, xLock);
        return lockService.lock(keyInfo, xLock.lockType(), () -> {
            try {
                return joinPoint.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new Exception(e);
            }
        });
    }

}
