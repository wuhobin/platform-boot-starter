package com.aurora.example.quartz;

import com.aurora.starter.common.utils.SpringUtils;
import com.aurora.starter.redis.core.RedisBloomFilter;
import com.aurora.starter.redis.core.manager.TwoLevelCacheManager;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.Map;

/**
 * 聚合测试配置 —— 把 InMemoryJobLogHandler 注入到主上下文.
 * 同时提供空的 BloomFilter Map 与 TwoLevelCacheManager 桩,
 * 避免 redis 子模块 Demo 控制器在测试上下文启动失败.
 * 显式注册 SpringUtils —— quartz JobInvokeUtils 依赖其 beanFactory 已初始化.
 */
@TestConfiguration
@Import(InMemoryJobLogHandler.class)
public class QuartzTestConfig {

    @Bean(name = "redisBloomFilters")
    public Map<String, RedisBloomFilter<?>> redisBloomFilters() {
        return Collections.emptyMap();
    }

    @Bean
    public TwoLevelCacheManager twoLevelCacheManager() {
        // 桩对象:测试只关心 Quartz,TwoLevelCache 不被调用.
        return new TwoLevelCacheManager(null, Collections.emptyMap());
    }

    @Bean
    public SpringUtilsPostProcessor springUtilsPostProcessor() {
        return new SpringUtilsPostProcessor();
    }

    /**
     * 通过 BeanFactoryPostProcessor 在 SpringUtils 被调用之前初始化其静态字段.
     * SpringUtils 是 final 类,无法直接 @Bean 实例化.
     */
    static class SpringUtilsPostProcessor
            implements org.springframework.beans.factory.config.BeanFactoryPostProcessor,
                       org.springframework.context.ApplicationContextAware {

        private ApplicationContext applicationContext;

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            try {
                java.lang.reflect.Field f = SpringUtils.class.getDeclaredField("beanFactory");
                f.setAccessible(true);
                f.set(null, beanFactory);
            } catch (Exception e) {
                throw new IllegalStateException("无法初始化 SpringUtils.beanFactory", e);
            }
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            try {
                java.lang.reflect.Field f = SpringUtils.class.getDeclaredField("applicationContext");
                f.setAccessible(true);
                f.set(null, applicationContext);
            } catch (Exception e) {
                throw new IllegalStateException("无法初始化 SpringUtils.applicationContext", e);
            }
        }
    }
}