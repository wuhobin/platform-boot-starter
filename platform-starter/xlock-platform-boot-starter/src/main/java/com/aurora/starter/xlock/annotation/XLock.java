package com.aurora.starter.xlock.annotation;


import com.aurora.starter.xlock.model.XLockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 锁的注解.
 *
 * @author breggor
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface XLock {

    /**
     * 自定义业务key前缀.
     *
     * @return 自定义业务key前缀
     */
    String prefix();

    /**
     * 使用spel:自定义业务key.
     *
     * @return 自定义业务key
     */
    String[] keys();

    /**
     * 锁类型.
     *
     * @return DisLockType
     */
    XLockType lockType() default XLockType.REENTRANT;

    /**
     * 加锁时间，超过该时长自动解锁，默认单位为：秒.
     *
     * @return long
     */
    long leaseTime() default -1;

    /**
     * 等待锁时间，默认单位：秒.
     *
     * @return long
     */
    long waitTime() default -1;

    /**
     * 锁时长单位.
     *
     * @return TimeUnit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 锁异常错误信息.
     */
    String errorMessage() default "";

    /**
     * 禁用日志，锁日志过多，正常情况下停止打印日志.
     */
    boolean disableLog() default false;
}
