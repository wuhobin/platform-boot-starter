package com.aurora.starter.xlock.config;

import com.aurora.starter.xlock.interceptor.XLockInterceptor;
import com.aurora.starter.xlock.interceptor.XLockSpelResolver;
import com.aurora.starter.xlock.lock.*;
import com.aurora.starter.xlock.service.LockFactory;
import com.aurora.starter.xlock.service.LockService;
import org.redisson.api.RedissonClient;
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
     */
    @Bean
    @ConditionalOnMissingBean
    public ReentrantLock reentrantLock(final RedissonClient redissonClient) {
        return new ReentrantLock(redissonClient);
    }

    /**
     * 公平锁操作服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public FairLock fairLock(final RedissonClient redissonClient) {
        return new FairLock(redissonClient);
    }

    /**
     * 联锁操作服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiLock multiLock(final RedissonClient redissonClient) {
        return new MultiLock(redissonClient);
    }

    /**
     * 红锁操作服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public RedLock redLock(final RedissonClient redissonClient) {
        return new RedLock(redissonClient);
    }

    /**
     * 读锁操作服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReadLock readLock(final RedissonClient redissonClient) {
        return new ReadLock(redissonClient);
    }

    /**
     * 写锁操作服务.
     */
    @Bean
    @ConditionalOnMissingBean
    public WriteLock writeLock(final RedissonClient redissonClient) {
        return new WriteLock(redissonClient);
    }

    /**
     * spring el解析key策略.
     */
    @Bean
    @ConditionalOnMissingBean
    public XLockSpelResolver xLockSpelResolver() {
        return new XLockSpelResolver();
    }

    /**
     * 锁拦截器.
     */
    @Bean
    @ConditionalOnMissingBean
    public XLockInterceptor xLockInterceptor() {
        return new XLockInterceptor();
    }

}
