package com.aurora.starter.oss.config;

import com.aurora.starter.oss.validation.FileUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadValidationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileUploadValidationAutoConfiguration.class));

    @Test
    void createsValidatorWithSafeDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileUploadValidationProperties.class);
            assertThat(context).hasSingleBean(FileUploadValidator.class);

            FileUploadValidationProperties properties =
                    context.getBean(FileUploadValidationProperties.class);
            assertThat(properties.getMaxSize()).isEqualTo(
                    DataSize.ofMegabytes(50));
            assertThat(properties.getMaxFilenameLength()).isEqualTo(255);
            assertThat(properties.getAllowedContentTypes())
                    .containsExactly(
                            "image/jpeg",
                            "image/png",
                            "image/gif",
                            "image/webp",
                            "video/mp4",
                            "application/pdf",
                            "application/zip",
                            "text/plain");
        });
    }

    @Test
    void bindsConfiguredValidationPolicy() {
        contextRunner
                .withPropertyValues(
                        "platform.oss.upload-validation.max-size=1KB",
                        "platform.oss.upload-validation.max-filename-length=64",
                        "platform.oss.upload-validation.allowed-content-types[0]=image/svg+xml",
                        "platform.oss.upload-validation.allowed-content-types[1]=image/avif")
                .run(context -> {
                    FileUploadValidationProperties properties =
                            context.getBean(FileUploadValidationProperties.class);
                    assertThat(properties.getMaxSize()).isEqualTo(DataSize.ofKilobytes(1));
                    assertThat(properties.getMaxFilenameLength()).isEqualTo(64);
                    assertThat(properties.getAllowedContentTypes())
                            .containsExactly(
                                    "image/jpeg",
                                    "image/png",
                                    "image/gif",
                                    "image/webp",
                                    "video/mp4",
                                    "application/pdf",
                                    "application/zip",
                                    "text/plain",
                                    "image/svg+xml",
                                    "image/avif");
                });
    }

    @Test
    void backsOffForAnApplicationProvidedValidator() {
        FileUploadValidator custom = new FileUploadValidator();

        contextRunner
                .withBean(FileUploadValidator.class, () -> custom)
                .run(context -> assertThat(context.getBean(FileUploadValidator.class))
                        .isSameAs(custom));
    }
}
