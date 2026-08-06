package com.aurora.starter.webmvc.exception;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void logsTheCompleteExceptionChainWhenABusinessExceptionHasACause() {
        IllegalStateException cause = new IllegalStateException("socket connection refused");
        BizException exception = new BizException("SSH connection failed", cause);

        LogEvent event = capture(() -> exceptionHandler.handleBizException(exception));

        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getMessage().getFormattedMessage())
                .isEqualTo("业务异常: code=500, message=SSH connection failed");
        assertThat(event.getThrown()).isSameAs(exception);
        assertThat(event.getThrown().getCause()).isSameAs(cause);
    }

    @Test
    void keepsExpectedBusinessExceptionsAsMessageOnlyLogs() {
        LogEvent event = capture(() ->
                exceptionHandler.handleBizException(new BizException(400, "invalid request")));

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getMessage().getFormattedMessage())
                .isEqualTo("业务异常: code=400, message=invalid request");
        assertThat(event.getThrown()).isNull();
    }

    private static LogEvent capture(Runnable action) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig loggerConfig = configuration.getLoggerConfig(
                GlobalExceptionHandler.class.getName());
        Level originalLevel = loggerConfig.getLevel();
        CapturingAppender appender = new CapturingAppender();
        appender.start();
        loggerConfig.setLevel(Level.ALL);
        loggerConfig.addAppender(appender, null, null);
        context.updateLoggers();
        try {
            action.run();
            assertThat(appender.events).hasSize(1);
            return appender.events.getFirst();
        } finally {
            loggerConfig.removeAppender(appender.getName());
            loggerConfig.setLevel(originalLevel);
            appender.stop();
            context.updateLoggers();
        }
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("global-exception-handler-test", null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (GlobalExceptionHandler.class.getName().equals(event.getLoggerName())) {
                events.add(event.toImmutable());
            }
        }
    }
}
