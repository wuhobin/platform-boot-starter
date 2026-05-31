package com.aurora.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例工程启动类.
 *
 * @author whb
 */
@SpringBootApplication
@MapperScan("com.aurora.example.mapper")
public class PlatformExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformExampleApplication.class, args);
    }
}