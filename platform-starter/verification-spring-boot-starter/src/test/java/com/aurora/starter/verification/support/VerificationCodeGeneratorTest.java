package com.aurora.starter.verification.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeGeneratorTest {

    @Test
    void generatesFixedLengthNumericCodes() {
        VerificationCodeGenerator generator = new VerificationCodeGenerator();

        for (int i = 0; i < 100; i++) {
            assertThat(generator.generate(6)).matches("\\d{6}");
        }
    }
}
