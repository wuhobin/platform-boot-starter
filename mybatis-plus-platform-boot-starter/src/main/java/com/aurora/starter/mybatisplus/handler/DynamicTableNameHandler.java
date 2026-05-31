package com.aurora.starter.mybatisplus.handler;

import cn.hutool.core.date.DateUtil;
import com.aurora.starter.common.constant.Constants;
import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.common.utils.threads.RequestThread;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
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
     * 用于记录哪些表可以使用该月份动态表名处理器.
     */
    private final List<String> tableNames;

    public DynamicTableNameHandler(String... tableNames) {
        this.tableNames = Arrays.asList(tableNames);
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
        // 沒有需要处理的表
        if (null == this.tableNames || this.tableNames.isEmpty()) {
            return tableName;
        }
        // 动态处理表名
        if (this.tableNames.contains(tableName)) {
            // 从线程中获取
            String suffix = RequestThread.getValue(Constants.DYNAMIC_TABLE_SUFFIX);
            if (StringUtils.isNotBlank(suffix)) {
                // 拼接动态表名
                return Constants.DYNAMIC_TABLE_DEFAULT_NAME.equals(suffix) ? tableName : tableName + SEPARATOR + suffix;
            }
            // 默认表名增加当前月份后缀
            return tableName + SEPARATOR + DateUtil.format(DateUtil.date(), MONTH_DATE_PATTERN);
        }
        return tableName;
    }

}
