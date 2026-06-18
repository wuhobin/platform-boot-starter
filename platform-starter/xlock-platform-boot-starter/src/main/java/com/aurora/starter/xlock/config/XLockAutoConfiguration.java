package com.aurora.starter.xlock.config;

import com.aurora.starter.xlock.interceptor.XLockInterceptor;
import com.aurora.starter.xlock.interceptor.XLockSpelResolver;
import com.aurora.starter.xlock.lock.*;
import com.aurora.starter.xlock.service.LockFactory;
import com.aurora.starter.xlock.service.LockService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 分布式锁自动配置.
 *
 * @author whb
 */
@AutoConfiguration
public class XLockAutoConfiguration {

    /**
     * 锁服务工厂.
     *
     * @return 锁服务工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public LockFactory serviceBeanFactory() {
        return new LockFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockService lockService() {
        return new LockService();
    }

    /**
     * 可重入锁加锁服务.
     *
     * @return 可重入锁加锁服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReentrantLock reentrantLock() {
        return new ReentrantLock();
    }

    /**
     * 公平锁操作服务.
     *
     * @return 公平锁操作服务
     */
    @Bean
    @ConditionalOnMissingBean
    public FairLock fairLock() {
        return new FairLock();
    }

    /**
     * 联锁操作服务.
     *
     * @return 联锁操作服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiLock multiLock() {
        return new MultiLock();
    }

    /**
     * 红锁操作服务.
     *
     * @return 红锁操作服务
     */
    @Bean
    @ConditionalOnMissingBean
    public RedLock redLock() {
        return new RedLock();
    }

    /**
     * 读锁操作服务.
     *
     * @return 读锁操作服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ReadLock readLock() {
        return new ReadLock();
    }

    /**
     * 写锁操作服务.
     *
     * @return 写锁操作服务
     */
    @Bean
    @ConditionalOnMissingBean
    public WriteLock writeLock() {
        return new WriteLock();
    }

    /**
     * spring el解析key策略.
     *
     * @return spring el解析key策略
     */
    @Bean
    @ConditionalOnMissingBean
    public XLockSpelResolver xLockSpelResolver() {
        return new XLockSpelResolver();
    }

    /**
     * 锁拦截器.
     *
     * @return 锁拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public XLockInterceptor xLockInterceptor() {
        return new XLockInterceptor();
    }

}
