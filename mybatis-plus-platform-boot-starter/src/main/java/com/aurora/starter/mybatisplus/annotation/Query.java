package com.aurora.starter.mybatisplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询标注注解.
 *
 * @author whb
 * @version 1.0
 * @date 2026-5-31 10:59:37
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

}
