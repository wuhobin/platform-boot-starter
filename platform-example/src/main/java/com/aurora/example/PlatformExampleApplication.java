package com.aurora.example;

import com.aurora.starter.webmvc.PlatformWebMvcMarker;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例工程启动类.
 *
 * <p>使用 {@code scanBasePackageClasses} 把 {@link PlatformWebMvcMarker} 所在的
 * {@code com.aurora.starter.webmvc} 包加入组件扫描，使 webmvc 通用件的
 * {@code GlobalExceptionHandler} / {@code TraceIdFilter} / {@code RequestLogFilter} 等 Bean 生效。</p>
 *
 * @author whb
 */
@SpringBootApplication(scanBasePackageClasses = {
        PlatformExampleApplication.class,
        PlatformWebMvcMarker.class
})
@MapperScan("com.aurora.example.mapper")
public class PlatformExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformExampleApplication.class, args);
    }
}
