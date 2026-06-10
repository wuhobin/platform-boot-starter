package com.aurora.starter.webmvc;

/**
 * 包扫描标记类.
 *
 * <p>下游业务工程可通过 {@code @SpringBootApplication(scanBasePackageClasses = ...)}
 * 引用本类，让 Spring 把 {@code com.aurora.starter.webmvc} 包加入组件扫描根，
 * 自动注册本模块的 {@link com.aurora.starter.webmvc.exception.GlobalExceptionHandler}、
 * {@link com.aurora.starter.webmvc.trace.TraceIdFilter}、
 * {@link com.aurora.starter.webmvc.log.RequestLogFilter} 等 Bean。</p>
 *
 * <p>本类不持有任何状态或方法，仅作包扫描锚点。</p>
 *
 * @author whb
 */
public final class PlatformWebMvcMarker {

    private PlatformWebMvcMarker() {
    }
}
