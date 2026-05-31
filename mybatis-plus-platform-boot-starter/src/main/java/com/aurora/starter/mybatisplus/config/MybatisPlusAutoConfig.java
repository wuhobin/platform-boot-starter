package com.aurora.starter.mybatisplus.config;

import com.aurora.starter.mybatisplus.handler.DynamicTableNameHandler;
import com.aurora.starter.mybatisplus.interceptor.CustomInterceptor;
import com.aurora.starter.mybatisplus.interceptor.FullScanInterceptor;
import com.aurora.starter.mybatisplus.mybatis.MetaObjectHandlerAdapter;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MybatisPlusAutoConfig.
 *
 * @author whb
 */
@Configuration
@EnableConfigurationProperties(MpExtProperties.class)
public class MybatisPlusAutoConfig {

    @Resource
    private MpExtProperties mpExtProperties;

    /**
     * 分页器、乐观锁.
     *
     * @return PaginationInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<CustomInterceptor> provider) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        // 乐观锁
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 阻断解析器,防止全表更新与删除
        mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 分页配置
        if (mpExtProperties.isPagePluginEnable()) {
            mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        }
        // 自定义拦截器
        mybatisPlusInterceptor.addInnerInterceptor(new FullScanInterceptor(mpExtProperties.getDisableFullScanTable()));
        // 动态表名
        final MpExtProperties.DynamicTableProperties dynamicTableProperties = mpExtProperties.getDynamicTable();
        if (dynamicTableProperties.isEnable()) {
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler(new DynamicTableNameHandler(dynamicTableProperties.getTables()));
            mybatisPlusInterceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        }

        provider.ifAvailable(i -> i.customInterceptor(mybatisPlusInterceptor));
        return mybatisPlusInterceptor;
    }

    /**
     * metaObjectHandler.
     *
     * @return MetaObjectHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandlerAdapter();
    }

}
