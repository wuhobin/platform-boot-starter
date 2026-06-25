package com.aurora.starter.security.log;

import cn.dev33.satoken.log.SaLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 将 Sa-Token log 信息转接到 Slf4j
 * <p>
 * 注册为 Spring Bean 后，
 * sa-token-spring-boot3-starter 内置的 SaLog BeanPostProcessor 会自动将其安装到 SaLogContext，
 * 框架内所有 SaLog 调用最终落到当前 Logger 关联的日志框架（Logback / Log4j2 ...），
 * 与业务日志统一输出并受业务日志级别控制。
 * </p>
 */
@Component
public class SaLogForSlf4j implements SaLog {

    Logger log = LoggerFactory.getLogger(SaLogForSlf4j.class);

    @Override
    public void trace(String str, Object... args) {
        log.trace(str, args);
    }

    @Override
    public void debug(String str, Object... args) {
        log.debug(str, args);
    }

    @Override
    public void info(String str, Object... args) {
        log.info(str, args);
    }

    @Override
    public void warn(String str, Object... args) {
        log.warn(str, args);
    }

    @Override
    public void error(String str, Object... args) {
        log.error(str, args);
    }

    @Override
    public void fatal(String str, Object... args) {
        log.error(str, args);
    }
}
