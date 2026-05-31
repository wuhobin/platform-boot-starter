package com.aurora.starter.mybatisplus.config;

import com.aurora.starter.mybatisplus.handler.DynamicTableNameHandler;
import com.aurora.starter.mybatisplus.interceptor.CustomInterceptor;
import com.aurora.starter.mybatisplus.interceptor.FullScanInterceptor;
import com.aurora.starter.mybatisplus.mybatis.MetaObjectHandlerAdapter;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlusAutoConfig.
 *
 * @author whb
 */
@Configuration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties(MpExtProperties.class)
public class MybatisPlusAutoConfig {

    private final MpExtProperties mpExtProperties;

    public MybatisPlusAutoConfig(final MpExtProperties mpExtProperties) {
        this.mpExtProperties = mpExtProperties;
    }

    /**
     * MyBatis-Plus 拦截器链.
     * <p>顺序按官方建议：分页 → 动态表名 → 乐观锁 → 阻断 → 自定义全表扫描。</p>
     * <p>多个 {@link CustomInterceptor} 会按 {@link org.springframework.core.annotation.Order @Order} 顺序追加。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<CustomInterceptor> customInterceptors) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();

        // 1. 分页配置（官方要求放第一位，恒开）
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 2. 动态表名
        final MpExtProperties.DynamicTableProperties dynamicTableProperties = mpExtProperties.getDynamicTable();
        if (dynamicTableProperties.isEnable()) {
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler(
                    new DynamicTableNameHandler(dynamicTableProperties.isFallbackToCurrentMonth(),
                            dynamicTableProperties.getTables()));
            mybatisPlusInterceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        }
        // 3. 乐观锁
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 4. 阻断解析器,防止全表更新与删除
        mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 5. 自定义全表扫描拦截
        mybatisPlusInterceptor.addInnerInterceptor(new FullScanInterceptor(mpExtProperties.getDisableFullScanTable()));

        // 业务侧追加扩展（支持多个 Bean，按 @Order 排序）
        customInterceptors.orderedStream().forEach(i -> i.customInterceptor(mybatisPlusInterceptor));
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
