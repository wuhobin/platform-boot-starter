package com.aurora.starter.oss.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatedMultipartFileTest {

    @Test
    void replacesOnlyTheContentTypeAndDelegatesTheMultipartFile() throws IOException {
        MockMultipartFile delegate = new MockMultipartFile(
                "file", "note.txt", "text/html", "hello".getBytes(StandardCharsets.UTF_8));

        ValidatedMultipartFile validated = new ValidatedMultipartFile(delegate, "text/plain");

        assertThat(validated.getName()).isEqualTo(delegate.getName());
        assertThat(validated.getOriginalFilename()).isEqualTo(delegate.getOriginalFilename());
        assertThat(validated.getContentType()).isEqualTo("text/plain");
        assertThat(validated.getSize()).isEqualTo(delegate.getSize());
        assertThat(validated.getBytes()).isEqualTo(delegate.getBytes());
    }
}
