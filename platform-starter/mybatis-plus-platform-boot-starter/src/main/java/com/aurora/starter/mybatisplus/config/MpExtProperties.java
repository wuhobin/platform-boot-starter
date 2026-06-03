package com.aurora.starter.mybatisplus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 扩展配置.
 *
 * @author whb
 * @date 2026-5-31 19:30
 */
@Data
@ConfigurationProperties(prefix = MpExtProperties.MP_EXT_PREFIX)
public class MpExtProperties {

    public static final String MP_EXT_PREFIX = "mybatis-plus.ext";

    /**
     * 动态表名配置.
     */
    private DynamicTableProperties dynamicTable = new DynamicTableProperties();

    /**
     * 禁止全表扫描的表名
     */
    private List<String> disableFullScanTable;

    /**
     * 动态表名相关配置.
     */
    @Data
    public static class DynamicTableProperties {

        /**
         * 是否启用动态表名模式.
         * 默认 false 不启用
         */
        private boolean enable;

        /**
         * 需要动态表名的表.
         */
        private String[] tables;

        /**
         * 线程未指定后缀时，是否回退到当前月份（yyyyMM）作为后缀.
         * 默认 false，即回退到原表名不加后缀.
         */
        private boolean fallbackToCurrentMonth;

    }

}
