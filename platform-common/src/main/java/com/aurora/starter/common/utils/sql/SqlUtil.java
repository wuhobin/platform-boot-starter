package com.aurora.starter.common.utils.sql;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.util.ArrayUtil;
import com.aurora.starter.common.utils.StringUtils;

/**
 * sql操作工具类
 *
 * @author author
 */
public class SqlUtil {

    /**
     * 定义常用的 sql关键字.
     */
    public static final String SQL_REGEX = "select |insert |delete |update |drop |count |exec |chr |mid |master |truncate |char |and |declare ";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）.
     */
    public static final String SQL_PATTERN = "[a-zA-Z0-9_ ,.+\\\\-]+";

    /**
     * 正序.
     */
    public static final String ASC_SUFFIX = " asc, ";

    /**
     * 倒序.
     */
    public static final String DESC_SUFFIX = " desc, ";

    /** 排序语法分隔符. */
    private static final String COMMA = ",";

    /** 升序前缀（后端约定）. */
    private static final String PLUS = "+";

    /** 降序前缀（后端约定）. */
    private static final String REDUCE = "-";

    /** 末尾分隔符片段，去除时用. */
    private static final String SPACE = " ";

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value) {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value)) {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value) {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords) {
            if (StringUtils.indexOfIgnoreCase(value, sqlKeyword) > -1) {
                throw new UtilException("参数存在SQL注入风险");
            }
        }
    }

    /**
     * 排序转换 标准SQL排序 语句.
     * eg：字段1,字段2 asc,字段3,字段4,字段5 desc
     *
     * @param sort sort
     * @return 结果
     */
    public static String convertSqlSort(final String sort) {
        // 判断是否为空
        if (StringUtils.isBlank(sort)) {
            return sort;
        }
        // 是否为后端写法格式 eg：-update_time,+id,-order_by
        if (sort.contains(REDUCE) || sort.contains(PLUS)) {
            // 按逗号拆分
            String[] sorts = sort.split(COMMA);
            if (ArrayUtil.isEmpty(sorts)) {
                return sort;
            }
            // 数据处理
            StringBuilder stringBuilder = new StringBuilder();
            for (String item : sorts) {
                // 正序
                if (item.startsWith(PLUS)) {
                    stringBuilder.append(StringUtils.toUnderScoreCase(item.substring(1))).append(ASC_SUFFIX);
                }
                // 倒序
                if (item.startsWith(REDUCE)) {
                    stringBuilder.append(StringUtils.toUnderScoreCase(item.substring(1))).append(DESC_SUFFIX);
                }
            }
            // 移除最后一个逗号和空格
            if (stringBuilder.toString().endsWith(SPACE)) {
                stringBuilder.setLength(stringBuilder.length() - 2);
            }
            return stringBuilder.toString();
        }
        return sort;
    }

}
