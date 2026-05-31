package com.aurora.starter.mybatisplus.handler;

import cn.hutool.core.date.DateUtil;
import com.aurora.starter.common.constant.Constants;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.threads.RequestThread;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 动态表名处理器.
 *
 * @author whb
 * @date 2023-08-01 19:26
 */
@Slf4j
public class DynamicTableNameHandler implements TableNameHandler {

    /**
     * 下划线.
     */
    private static final char SEPARATOR = '_';

    /**
     * 标准日期格式：yyyyMM.
     */
    private static final String MONTH_DATE_PATTERN = "yyyyMM";

    /**
     * 用于记录哪些表可以使用该动态表名处理器.
     */
    private final List<String> tableNames;

    /**
     * 线程未指定后缀时，是否回退到当前月份后缀.
     * false 时回退到原表名，避免误访问未建立的月份分表.
     */
    private final boolean fallbackToCurrentMonth;

    public DynamicTableNameHandler(String... tableNames) {
        this(false, tableNames);
    }

    public DynamicTableNameHandler(boolean fallbackToCurrentMonth, String... tableNames) {
        this.fallbackToCurrentMonth = fallbackToCurrentMonth;
        this.tableNames = tableNames == null ? Collections.emptyList() : Arrays.asList(tableNames);
    }

    /**
     * 生成动态表名.
     *
     * @param sql       当前执行SQL
     * @param tableName 表名
     * @return String
     */
    @Override
    public String dynamicTableName(final String sql, final String tableName) {
        if (this.tableNames.isEmpty() || !this.tableNames.contains(tableName)) {
            return tableName;
        }
        // 从线程中获取后缀
        String suffix = RequestThread.getValue(Constants.DYNAMIC_TABLE_SUFFIX);
        if (StringUtils.isNotBlank(suffix)) {
            return Constants.DYNAMIC_TABLE_DEFAULT_NAME.equals(suffix) ? tableName : tableName + SEPARATOR + suffix;
        }
        // 线程无后缀：按配置决定是否回退到当前月份
        if (fallbackToCurrentMonth) {
            return tableName + SEPARATOR + DateUtil.format(DateUtil.date(), MONTH_DATE_PATTERN);
        }
        return tableName;
    }

}
