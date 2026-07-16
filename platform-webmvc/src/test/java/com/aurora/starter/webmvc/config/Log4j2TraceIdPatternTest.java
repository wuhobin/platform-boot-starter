package com.aurora.starter.webmvc.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2TraceIdPatternTest {

    @Test
    void rendersTraceIdFromMdc() throws Exception {
        String pattern = loadPattern();
        SortedArrayStringMap contextData = new SortedArrayStringMap();
        contextData.putValue("traceId", "trace-123");
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("test.logger")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("test message"))
                .setContextData(contextData)
                .build();

        String logLine = PatternLayout.newBuilder()
                .withPattern(pattern)
                .build()
                .toSerializable(event);

        assertThat(logLine).contains("[trace-123]");
    }

    private static String loadPattern() throws Exception {
        try (InputStream input = Log4j2TraceIdPatternTest.class.getResourceAsStream("/log4j2-spring.xml")) {
            assertThat(input).isNotNull();
            NodeList properties = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .getElementsByTagName("Property");
            for (int i = 0; i < properties.getLength(); i++) {
                Element property = (Element) properties.item(i);
                if ("PATTERN".equals(property.getAttribute("name"))) {
                    return property.getTextContent();
                }
            }
        }
        throw new IllegalStateException("PATTERN property not found");
    }
}
