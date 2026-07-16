package com.aurora.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例工程启动类.
 *
 * <p>使用 {@code scanBasePackageClasses} 把 webmvc / quartz 等 starter 的组件纳入扫描，
 * 使 {@code GlobalExceptionHandler} / {@code IQuartzJobService} 等 Bean 生效。</p>
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
