package com.aurora.starter.verification.image;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.common.response.ApiResponseStatusConstant;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.aurora.starter.verification.exception.ImageVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultImageVerificationServiceTest {

    @Mock
    private SecondaryVerificationApplication application;

    private DefaultImageVerificationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultImageVerificationService(application, "slider");
    }

    @Test
    void generatesConfiguredTypeAndUnwrapsResponse() {
        ImageCaptchaVO captcha = new ImageCaptchaVO();
        captcha.setId("SLIDER_captcha-id");
        when(application.generateCaptcha("SLIDER"))
                .thenReturn(ApiResponse.ofSuccess(captcha));

        ImageCaptchaVO result = service.generate();

        assertThat(result).isSameAs(captcha);
        verify(application).generateCaptcha("SLIDER");
    }

    @Test
    void rejectsUnsuccessfulOrEmptyGenerationResponses() {
        when(application.generateCaptcha("SLIDER"))
                .thenReturn(ApiResponse.ofError("generator unavailable"));

        assertThatThrownBy(service::generate)
                .isInstanceOf(ImageVerificationException.class)
                .hasMessageContaining("generator unavailable");

        when(application.generateCaptcha("SLIDER"))
                .thenReturn(ApiResponse.ofSuccess(null));

        assertThatThrownBy(service::generate)
                .isInstanceOf(ImageVerificationException.class)
                .hasMessageContaining("Failed to generate");
    }

    @Test
    void wrapsGenerationInfrastructureFailure() {
        IllegalStateException failure = new IllegalStateException("cache unavailable");
        when(application.generateCaptcha("SLIDER")).thenThrow(failure);

        assertThatThrownBy(service::generate)
                .isInstanceOf(ImageVerificationException.class)
                .hasMessage("Failed to generate image captcha")
                .hasCause(failure);
    }

    @Test
    void matchesTrimmedCaptchaId() {
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        when(application.matching("captcha-id", track))
                .thenReturn(ApiResponse.ofSuccess());

        assertThat(service.match("  captcha-id  ", track)).isTrue();

        verify(application).matching("captcha-id", track);
    }

    @Test
    void returnsFalseForExpectedMatchFailures() {
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        when(application.matching("expired", track))
                .thenReturn(ApiResponse.ofMessage(ApiResponseStatusConstant.EXPIRED));
        when(application.matching("missing", track)).thenReturn(null);
        when(application.matching("malformed", track))
                .thenThrow(new IllegalArgumentException("invalid track"));

        assertThat(service.match("expired", track)).isFalse();
        assertThat(service.match("missing", track)).isFalse();
        assertThat(service.match("malformed", track)).isFalse();
    }

    @Test
    void rejectsInvalidMatchArgumentsBeforeCallingTianai() {
        assertThatThrownBy(() -> service.match("  ", new ImageCaptchaTrack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("captchaId");
        assertThatThrownBy(() -> service.match("captcha-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("track");

        verifyNoInteractions(application);
    }

    @Test
    void wrapsMatchingInfrastructureFailure() {
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        IllegalStateException failure = new IllegalStateException("redis unavailable");
        when(application.matching("captcha-id", track)).thenThrow(failure);

        assertThatThrownBy(() -> service.match("captcha-id", track))
                .isInstanceOf(ImageVerificationException.class)
                .hasMessage("Failed to match image captcha")
                .hasCause(failure);
    }

    @Test
    void verifiesAndConsumesTrimmedCredential() {
        when(application.secondaryVerification("captcha-id")).thenReturn(true);

        assertThat(service.verifyAndConsume("  captcha-id  ")).isTrue();

        verify(application).secondaryVerification("captcha-id");
    }

    @Test
    void returnsFalseForExpiredOrAlreadyConsumedCredential() {
        when(application.secondaryVerification("captcha-id")).thenReturn(false);

        assertThat(service.verifyAndConsume("captcha-id")).isFalse();
    }

    @Test
    void validatesConstructorAndCredentialArguments() {
        ImageCaptchaApplication undecoratedApplication = mock(ImageCaptchaApplication.class);

        assertThatThrownBy(() -> new DefaultImageVerificationService(
                undecoratedApplication, "SLIDER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secondary verification");
        assertThatThrownBy(() -> new DefaultImageVerificationService(application, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
        assertThatThrownBy(() -> service.verifyAndConsume(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("captchaId");

        verify(application, never()).secondaryVerification(" ");
    }
}
