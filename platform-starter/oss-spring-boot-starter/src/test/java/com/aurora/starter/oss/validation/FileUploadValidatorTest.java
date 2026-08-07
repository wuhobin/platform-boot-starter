package com.aurora.starter.oss.validation;

import com.aurora.starter.oss.config.FileUploadValidationProperties;
import com.aurora.starter.oss.exception.FileValidationException;
import com.aurora.starter.oss.exception.FileValidationReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadValidatorTest {

    private final FileUploadValidator validator = new FileUploadValidator();

    @Test
    void acceptsContentWhenTheDetectedTypeMatchesTheConfiguredExtension() {
        String contentType = validator.validate(file("document.pdf", pdfBytes()));

        assertThat(contentType).isEqualTo("application/pdf");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultFiles")
    void acceptsEveryDefaultFileFormat(String filename, byte[] content, String expectedContentType) {
        assertThat(validator.validate(file(filename, content)))
                .isEqualTo(expectedContentType);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonMp4FtypBrands")
    void rejectsOtherIsoBaseMediaFormatsRenamedAsMp4(String majorBrand) {
        assertThatThrownBy(() -> validator.validate(file(
                "video.mp4", ftypBytes(majorBrand))))
                .isInstanceOfSatisfying(FileValidationException.class, exception -> {
                    assertThat(exception.getReason())
                            .isEqualTo(FileValidationReason.CONTENT_TYPE_MISMATCH);
                    assertThat(exception.getDetectedContentType())
                            .isNotEqualTo("video/mp4");
                });
    }

    @Test
    void rejectsContentDisguisedWithAnAllowedExtension() {
        assertThatThrownBy(() -> validator.validate(file(
                "document.pdf", "plain text".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOfSatisfying(FileValidationException.class, exception -> {
                    assertThat(exception.getReason())
                            .isEqualTo(FileValidationReason.CONTENT_TYPE_MISMATCH);
                    assertThat(exception.getExpectedContentTypes())
                            .containsExactly("application/pdf");
                    assertThat(exception.getDetectedContentType()).isEqualTo("text/plain");
                });
    }

    @Test
    void rejectsAnUnsupportedExtensionBeforeReadingTheContent() {
        assertThatThrownBy(() -> validator.validate(file("document.bmp", pdfBytes())))
                .isInstanceOfSatisfying(FileValidationException.class, exception ->
                        assertThat(exception.getReason())
                                .isEqualTo(FileValidationReason.EXTENSION_NOT_ALLOWED));
    }

    @Test
    void enforcesTheConfiguredSizeAndFilenameLimits() {
        FileUploadValidationProperties sizeProperties = new FileUploadValidationProperties();
        sizeProperties.setMaxSize(DataSize.ofBytes(4));
        FileUploadValidator sizeLimitedValidator = new FileUploadValidator(sizeProperties);

        assertThatThrownBy(() -> sizeLimitedValidator.validate(file("a.png", new byte[5])))
                .isInstanceOfSatisfying(FileValidationException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(FileValidationReason.TOO_LARGE));

        FileUploadValidationProperties filenameProperties = new FileUploadValidationProperties();
        filenameProperties.setMaxFilenameLength(8);
        FileUploadValidator filenameLimitedValidator = new FileUploadValidator(filenameProperties);
        assertThatThrownBy(() -> filenameLimitedValidator.validate(
                file("123456789.png", pngBytes())))
                .isInstanceOfSatisfying(FileValidationException.class, exception ->
                        assertThat(exception.getReason())
                                .isEqualTo(FileValidationReason.FILENAME_TOO_LONG));
    }

    @Test
    void allowsAConfiguredMimeTypeInAdditionToTheDefaults() {
        FileUploadValidationProperties properties = new FileUploadValidationProperties();
        properties.setAllowedContentTypes(List.of("image/svg+xml"));
        FileUploadValidator configuredValidator = new FileUploadValidator(properties);

        assertThat(configuredValidator.expectedContentTypes("image.svg"))
                .containsExactly("image/svg+xml");
        assertThat(configuredValidator.validate(file("image.svg", svgBytes())))
                .isEqualTo("image/svg+xml");
        assertThat(configuredValidator.validate(file(
                "notes.txt", "hello".getBytes(StandardCharsets.UTF_8))))
                .isEqualTo("text/plain");
    }

    @Test
    void rejectsEmptyFiles() {
        assertThatThrownBy(() -> validator.validate(file("empty.txt", new byte[0])))
                .isInstanceOfSatisfying(FileValidationException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(FileValidationReason.EMPTY));
    }

    private static MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    private static byte[] pdfBytes() {
        return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a
        };
    }

    private static Stream<Arguments> defaultFiles() {
        return Stream.of(
                Arguments.of("photo.jpg", jpegBytes(), "image/jpeg"),
                Arguments.of("photo.jpeg", jpegBytes(), "image/jpeg"),
                Arguments.of("image.png", pngBytes(), "image/png"),
                Arguments.of("animation.gif", "GIF89a".getBytes(StandardCharsets.US_ASCII), "image/gif"),
                Arguments.of("image.webp", webpBytes(), "image/webp"),
                Arguments.of("video.mp4", ftypBytes("isom"), "video/mp4"),
                Arguments.of("archive.zip", zipBytes(), "application/zip"),
                Arguments.of("notes.txt", "Nexora upload test".getBytes(StandardCharsets.UTF_8), "text/plain")
        );
    }

    private static Stream<String> nonMp4FtypBrands() {
        return Stream.of("qt  ", "avif", "heic", "3gp6", "M4A ");
    }

    private static byte[] jpegBytes() {
        return new byte[]{
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
                0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
        };
    }

    private static byte[] webpBytes() {
        return new byte[]{
                'R', 'I', 'F', 'F', 0x04, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
    }

    private static byte[] svgBytes() {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"10\" height=\"10\">"
                + "<rect width=\"10\" height=\"10\"/></svg>")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] ftypBytes(String majorBrand) {
        byte[] brand = majorBrand.getBytes(StandardCharsets.US_ASCII);
        return new byte[]{
                0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
                brand[0], brand[1], brand[2], brand[3], 0x00, 0x00, 0x02, 0x00,
                'i', 's', 'o', 'm', 'i', 's', 'o', '2'
        };
    }

    private static byte[] zipBytes() {
        return new byte[]{
                'P', 'K', 0x05, 0x06,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
