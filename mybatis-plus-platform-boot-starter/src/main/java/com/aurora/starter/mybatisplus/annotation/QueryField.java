package com.aurora.starter.mybatisplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询方式 加上注解的属性为查询字段.
 *
 * @author whb
 * @version 1.0
 * @date 2026-5-31 09:58
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {

    /**
     * 查询方式.
     * 默认为全匹配
     *
     * @return Operator
     */
    Operator operator() default Operator.EQ;

    /**
     * 查询字段名.
     * 不填写默认为注解属性名
     * 如果配置{@link #orFields()}，则可以不填写，同时填写的情况下会把该字段加到or查询字段中
     *
     * @return 结果
     */
    String field() default "";

    /**
     * OR查询的字段名集合.
     * 配置后会查询指定的多个字段（条件相同多字段or查询）
     * eg：(fieldA {@link #operator()} ? OR fieldB {@link #operator()} ? OR fieldC {@link #operator()} ?)
     *
     * @return 结果
     */
    String[] orFields() default {};

    /**
     * 多字段名集合.
     * {@link #operator()} 为 #{@link Operator#GROUP} 和 #{@link Operator#DISTINCT} 时生效
     *
     * @return 结果
     */
    String[] fields() default {};

    /**
     * 是否查询子文档.
     * 仅支持MangoDB查询
     *
     * @return boolean
     */
    boolean queryInner() default false;

    /**
     * 子文档名称.
     * 仅支持MangoDB查询
     *
     * @return 结果
     */
    String innerName() default "";

    /**
     * 是否查询空字符串.
     * 默认不查询
     *
     * @return 结果
     */
    boolean queryEmpty() default false;

    /**
     * 是否忽略该字段.
     * 默认不忽略
     *
     * @return 结果
     */
    boolean ignore() default false;



}
