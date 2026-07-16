package com.aurora.starter.webmvc.domain.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ResultContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void serializesCanonicalResponseFields() {
        MDC.put("traceId", "trace-123");

        Result<String> result = Result.data("payload").putExtra("page", 1);
        JsonNode json = objectMapper.valueToTree(result);

        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("message").asText()).isEqualTo("success");
        assertThat(json.get("data").asText()).isEqualTo("payload");
        assertThat(json.get("extra").get("page").asInt()).isEqualTo(1);
        assertThat(json.get("traceId").asText()).isEqualTo("trace-123");
        assertThat(json.has("msg")).isFalse();
    }

    @Test
    void legacyMsgAccessorsDelegateToMessage() {
        Result<Void> result = new Result<>();

        result.setMsg("legacy");

        assertThat(result.getMessage()).isEqualTo("legacy");
        assertThat(result.getMsg()).isEqualTo("legacy");
    }
}
