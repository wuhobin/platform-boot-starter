package com.aurora.starter.mybatisplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询字段注解. 标注在查询对象的字段上，配合 {@link com.aurora.starter.mybatisplus.mybatis.DynamicCondition}
 * 使用，会根据 {@link #operator()} 生成对应的 MyBatis-Plus QueryWrapper 条件.
 *
 * @author whb
 * @version 1.0
 * @date 2026-5-31 09:58
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {

    /**
     * 查询操作符.
     * 默认为 EQ（等于）.
     *
     * @return Operator
     */
    Operator operator() default Operator.EQ;

    /**
     * 映射的数据库列名.
     * 不填写时默认使用字段名（驼峰自动转下划线）.
     *
     * @return 列名
     */
    String field() default "";

    /**
     * OR 查询字段集合.
     * 配置后生成 (fieldA op ? OR fieldB op ? OR fieldC op ?) 模式的 OR 组合.
     *
     * @return OR 字段名数组
     */
    String[] orFields() default {};

    /**
     * 多字段集合.
     * 用于 GROUP / DISTINCT 等需要多字段的操作.
     *
     * @return 字段名数组
     */
    String[] fields() default {};

    /**
     * 是否查询空字符串.
     * 默认为 false，即空字符串不参与查询条件.
     *
     * @return boolean
     */
    boolean queryEmpty() default false;

    /**
     * 是否忽略该字段.
     * 默认为 false，即该字段不参与动态条件构造.
     *
     * @return boolean
     */
    boolean ignore() default false;

}