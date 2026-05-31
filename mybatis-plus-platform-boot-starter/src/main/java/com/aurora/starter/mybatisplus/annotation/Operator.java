package com.aurora.starter.mybatisplus.annotation;

/**
 * 查询操作符.
 *
 * @author whv
 * @version 1.0
 * @date 2021-9-7 09:58
 */
public enum Operator {

    /**
     * 等于 =.
     */
    EQ,

    /**
     * 等于 =.
     */
    ENUM_EQ,

    /**
     * 不等于 <>.
     */
    NE,

    /**
     * 模糊查询 LIKE '%值%'.
     */
    LIKE,

    /**
     * 左模糊查询 LIKE '%值'.
     */
    LIKE_LEFT,

    /**
     * 右模糊查询 LIKE '值%'.
     */
    LIKE_RIGHT,

    /**
     * 模糊查询 NOT LIKE '%值%'.
     */
    NOT_LIKE,

    /**
     * 大于 >.
     */
    GT,

    /**
     * 小于 <.
     */
    LT,

    /**
     * 大于等于 >=.
     */
    GTE,

    /**
     * 小于等于 <=.
     */
    LTE,

    /**
     * 范围查询
     */
    BETWEEN,

    /**
     * 不在范围查询.
     */
    NOT_BETWEEN,

    /**
     * 不为空 IS NOT NULL.
     */
    NOT_NULL,

    /**
     * 不为空 或者 不为空字符.
     */
    NOT_EMPTY,

    /**
     * 不为空 并且 不为空字符.
     */
    NOT_AND_EMPTY,

    /**
     * 为空 IS NULL.
     */
    IS_NULL,

    /**
     * 为空 或者 为空字符.
     */
    IS_EMPTY,

    /**
     * 为空 并且 为空字符.
     */
    IS_AND_EMPTY,

    /**
     * IN查询 IN (v0, v1, ...).
     */
    IN,

    /**
     * NOT IN查询 NOT IN (v0, v1, ...).
     */
    NOT_IN,

    /**
     * json array 全匹配， JSON_CONTAINS(field, 'v0') and JSON_CONTAINS(field, 'v1').
     */
    JSON_ARRAY_ALL_MATCH,

    /**
     * json array 全匹配否定， not JSON_CONTAINS(field, CAST('v0' AS JSON)) and not JSON_CONTAINS(field, CAST('v1' AS JSON))
     */
    JSON_ARRAY_ALL_NOT_MATCH,

    /**
     * json array 全匹配， JSON_CONTAINS(field, CAST('v0' AS JSON)) AND JSON_CONTAINS(field, CAST('v1' AS JSON))
     */
    JSON_ARRAY_ALL_MATCH_DORIS,

    /**
     * json array 任意匹配，JSON_CONTAINS(field, 'v0') or JSON_CONTAINS(field, 'v1').
     */
    JSON_ARRAY_ANY_MATCH,

    /**
     * json array 任意匹配，JSON_CONTAINS(field, CAST('v0' AS JSON)) or JSON_CONTAINS(field, CAST('v1' AS JSON))
     */
    JSON_ARRAY_ANY_MATCH_DORIS,

    /**
     * json array 任意匹配，JSON_CONTAINS(field, 'v0') or JSON_CONTAINS(field, 'v1'). 或者 为空/null
     */
    JSON_ARRAY_ANY_MATCH_WITH_EMPTY,

    /**
     * json array 任意匹配，JSON_CONTAINS(field, CAST('v0' AS JSON)) or JSON_CONTAINS(field, CAST('v1' AS JSON)). 或者 为空/null
     */
    JSON_ARRAY_ANY_MATCH_WITH_EMPTY_DORIS,

    /**
     * 包含查询.
     * 某个值是否在以逗号分隔的字符串列表里
     *
     * <pre>
     * FIND_IN_SET(str,strList)
     * str 要查询的字符串 strList 参数以,分隔的字段名 如 (1,2,6,8,10,22)
     * 查询字段(strList)中包含(str)的结果，返回结果为null或记录
     * </pre>
     */
    FIND_IN_SET,

    /**
     * 分组 GROUP BY 字段1、字段2、、、.
     *
     * <pre>
     *     默认使用 #{@link QueryField#field} ，多个字段把对应字段放到 #{@link QueryField#fields} 中
     * </pre>
     */
    GROUP,

    /**
     * 限制返回数量.
     *
     * <pre>
     *     qw.last("limit 1") 限制返回数量
     *     只能使用一次，多次调用将以最后一次为准
     * </pre>
     */
    LIMIT,

    /**
     * 去重 DISTINCT 字段1、字段2、、、.
     *
     * <pre>
     *     默认使用 #{@link QueryField#field} ，多个字段把对应字段放到 #{@link QueryField#fields()} 中
     * </pre>
     */
    DISTINCT;

}
