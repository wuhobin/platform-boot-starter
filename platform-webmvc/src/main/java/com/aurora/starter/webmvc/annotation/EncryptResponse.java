package com.aurora.starter.webmvc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 加密接口响应中的 {@code Result.data} 字段。
 *
 * <p>可标注在 Controller 类型或方法上。该注解仅在响应加密功能开启时生效。</p>
 */
@Documented
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptResponse {
}
