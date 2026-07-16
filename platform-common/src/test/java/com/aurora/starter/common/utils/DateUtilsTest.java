package com.aurora.starter.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void exposesCompactDateFormatForStoragePaths() {
        assertThat(DateUtils.YYYYMMDD).isEqualTo("yyyyMMdd");
    }
}
