package com.aurora.starter.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void normalizesEmail() {
        assertThat(StringUtils.normalizeEmail(" User@Example.COM ")).isEqualTo("user@example.com");
        assertThat(StringUtils.normalizeEmail("")).isEmpty();
        assertThat(StringUtils.normalizeEmail(null)).isNull();
    }
}
