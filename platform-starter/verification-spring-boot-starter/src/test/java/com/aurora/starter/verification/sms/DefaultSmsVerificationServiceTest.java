package com.aurora.starter.verification.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.exception.SmsVerificationDeliveryException;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationRateLimitException;
import com.aurora.starter.verification.exception.VerificationRateLimitType;
import com.aurora.starter.verification.redis.RedisSmsVerificationRepository;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSmsVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-31T08:00:00Z");

    @Mock
    private Client client;

    @Mock
    private RedisSmsVerificationRepository repository;

    @Mock
    private VerificationCodeGenerator codeGenerator;

    private VerificationProperties properties;
    private DefaultSmsVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new VerificationProperties();
        properties.getSms().setEnabled(true);
        service = new DefaultSmsVerificationService(
                client,
                repository,
                codeGenerator,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsUsingOnlyFixedAliyunFieldsAndMapsCompleteResponse() throws Exception {
        prepareReservation();
        SendSmsVerifyCodeResponse aliyunResponse = successResponse();
        when(client.sendSmsVerifyCodeWithOptions(any(), any())).thenReturn(aliyunResponse);

        SmsVerificationSendResponse response = service.send(new SmsVerificationSendRequest(
                " +8613800138000 ",
                CommonVerificationScene.REGISTER));

        ArgumentCaptor<SendSmsVerifyCodeRequest> requestCaptor =
                ArgumentCaptor.forClass(SendSmsVerifyCodeRequest.class);
        ArgumentCaptor<RuntimeOptions> optionsCaptor = ArgumentCaptor.forClass(RuntimeOptions.class);
        verify(client).sendSmsVerifyCodeWithOptions(requestCaptor.capture(), optionsCaptor.capture());

        SendSmsVerifyCodeRequest request = requestCaptor.getValue();
        assertThat(request.getPhoneNumber()).isEqualTo("13800138000");
        assertThat(request.getSignName()).isEqualTo("恒创联众");
        assertThat(request.getTemplateCode()).isEqualTo("100001");
        assertThat(request.getTemplateParam()).isEqualTo("{\"code\":\"012345\",\"min\":\"5\"}");
        assertThat(request.getOutId()).isNotBlank();
        assertThat(request.getReturnVerifyCode()).isFalse();
        assertThat(request.getAutoRetry()).isZero();
        assertThat(request.getCountryCode()).isNull();
        assertThat(request.getCodeLength()).isNull();
        assertThat(request.getValidTime()).isNull();
        assertThat(request.getDuplicatePolicy()).isNull();
        assertThat(request.getInterval()).isNull();
        assertThat(request.getSchemeName()).isNull();
        assertThat(request.getCodeType()).isNull();
        assertThat(request.getSmsUpExtendCode()).isNull();

        RuntimeOptions options = optionsCaptor.getValue();
        assertThat(options.getConnectTimeout().intValue()).isEqualTo(3000);
        assertThat(options.getReadTimeout().intValue()).isEqualTo(5000);
        assertThat(options.getAutoretry()).isFalse();
        assertThat(options.getMaxAttempts().intValue()).isEqualTo(1);

        assertThat(response.headers()).containsEntry("x-acs-request-id", "header-request-id");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().success()).isTrue();
        assertThat(response.body().code()).isEqualTo("OK");
        assertThat(response.body().message()).isEqualTo("OK");
        assertThat(response.body().requestId()).isEqualTo("body-request-id");
        assertThat(response.body().accessDeniedDetail()).isEqualTo("none");
        assertThat(response.body().model().requestId()).isEqualTo("model-request-id");
        assertThat(response.body().model().outId()).isEqualTo("out-id");
        assertThat(response.body().model().bizId()).isEqualTo("biz-id");
        assertThat(response.toString()).doesNotContain("aliyun-returned-code");
        assertThatThrownBy(() -> response.headers().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rollsBackReservationWhenAliyunExplicitlyRejectsRequest() throws Exception {
        prepareReservation();
        SendSmsVerifyCodeResponse rejected = new SendSmsVerifyCodeResponse()
                .setStatusCode(200)
                .setBody(new SendSmsVerifyCodeResponseBody()
                        .setSuccess(false)
                        .setCode("isv.BUSINESS_LIMIT_CONTROL")
                        .setMessage("limit")
                        .setRequestId("request-id"));
        when(client.sendSmsVerifyCodeWithOptions(any(), any())).thenReturn(rejected);

        SmsVerificationDeliveryException exception = catchThrowableOfType(
                SmsVerificationDeliveryException.class,
                () -> service.send(sendRequest()));

        assertThat(exception.getResponse()).isNotNull();
        assertThat(exception.getResponse().body().code())
                .isEqualTo("isv.BUSINESS_LIMIT_CONTROL");
        verify(repository).rollback(
                eq("13800138000"),
                eq("LOGIN"),
                anyString(),
                eq(NOW));
    }

    @Test
    void preservesReservationWhenDeliveryResultIsUnknown() throws Exception {
        prepareReservation();
        when(client.sendSmsVerifyCodeWithOptions(any(), any()))
                .thenThrow(new IOException("timeout"));

        SmsVerificationDeliveryException exception = catchThrowableOfType(
                SmsVerificationDeliveryException.class,
                () -> service.send(sendRequest()));

        assertThat(exception.getResponse()).isNull();
        assertThat(exception).hasCauseInstanceOf(IOException.class);
        verify(repository, never()).rollback(anyString(), anyString(), anyString(), any());
    }

    @Test
    void reportsHourlyRateLimitWithoutCallingAliyun() throws Exception {
        when(codeGenerator.generate(6)).thenReturn("012345");
        when(repository.reserve(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new RedisSmsVerificationRepository.Reservation(
                        RedisSmsVerificationRepository.ReservationStatus.HOURLY_LIMIT,
                        Duration.ofMinutes(12)));

        assertThatThrownBy(() -> service.send(sendRequest()))
                .isInstanceOf(VerificationRateLimitException.class)
                .extracting("type", "retryAfter")
                .containsExactly(VerificationRateLimitType.HOURLY, Duration.ofMinutes(12));

        verify(client, never()).sendSmsVerifyCodeWithOptions(any(), any());
    }

    @Test
    void reportsCooldownWithoutCallingAliyun() throws Exception {
        when(codeGenerator.generate(6)).thenReturn("012345");
        when(repository.reserve(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new RedisSmsVerificationRepository.Reservation(
                        RedisSmsVerificationRepository.ReservationStatus.COOLDOWN,
                        Duration.ofSeconds(20)));

        assertThatThrownBy(() -> service.send(sendRequest()))
                .isInstanceOf(VerificationCooldownException.class)
                .extracting("retryAfter")
                .isEqualTo(Duration.ofSeconds(20));

        verify(client, never()).sendSmsVerifyCodeWithOptions(any(), any());
    }

    @Test
    void verifiesLocallyUsingNormalizedPhoneAndConfiguredAttemptLimit() {
        when(repository.verifyAndConsume("13800138000", "RESET_PASSWORD", "654321", 5))
                .thenReturn(new RedisSmsVerificationRepository.VerificationResult(true, 0));

        boolean result = service.verifyAndConsume(new SmsVerificationVerifyRequest(
                "8613800138000",
                CommonVerificationScene.RESET_PASSWORD,
                " 654321 "));

        assertThat(result).isTrue();
        verify(repository).verifyAndConsume("13800138000", "RESET_PASSWORD", "654321", 5);
    }

    @Test
    void rejectsInvalidMainlandPhoneBeforeReserving() {
        assertThatThrownBy(() -> service.send(new SmsVerificationSendRequest(
                "+85261234567",
                CommonVerificationScene.LOGIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mainland China");

        verify(repository, never()).reserve(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void rejectsCodeGeneratorOutputThatIsNotExactlySixDigits() {
        when(codeGenerator.generate(6)).thenReturn("12345A");

        assertThatThrownBy(() -> service.send(sendRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 6 digits");

        verify(repository, never()).reserve(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt());
    }

    private void prepareReservation() {
        when(codeGenerator.generate(6)).thenReturn("012345");
        when(repository.reserve(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new RedisSmsVerificationRepository.Reservation(
                        RedisSmsVerificationRepository.ReservationStatus.RESERVED,
                        Duration.ZERO));
    }

    private SmsVerificationSendRequest sendRequest() {
        return new SmsVerificationSendRequest(
                "13800138000",
                CommonVerificationScene.LOGIN);
    }

    private SendSmsVerifyCodeResponse successResponse() {
        SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel model =
                new SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel()
                        .setRequestId("model-request-id")
                        .setOutId("out-id")
                        .setBizId("biz-id")
                        .setVerifyCode("aliyun-returned-code");
        return new SendSmsVerifyCodeResponse()
                .setHeaders(Map.of("x-acs-request-id", "header-request-id"))
                .setStatusCode(200)
                .setBody(new SendSmsVerifyCodeResponseBody()
                        .setSuccess(true)
                        .setCode("OK")
                        .setMessage("OK")
                        .setRequestId("body-request-id")
                        .setAccessDeniedDetail("none")
                        .setModel(model));
    }
}
