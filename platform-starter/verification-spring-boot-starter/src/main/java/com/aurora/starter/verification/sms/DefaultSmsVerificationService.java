package com.aurora.starter.verification.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.exception.SmsVerificationDeliveryException;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationRateLimitException;
import com.aurora.starter.verification.exception.VerificationRateLimitType;
import com.aurora.starter.verification.exception.VerificationStorageException;
import com.aurora.starter.verification.redis.RedisSmsVerificationRepository;
import com.aurora.starter.verification.scene.VerificationScene;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于阿里云短信认证服务的默认短信验证码服务。
 */
@Slf4j
public class DefaultSmsVerificationService implements SmsVerificationService {

    static final String SIGN_NAME = "恒创联众";
    static final String TEMPLATE_CODE = "100001";
    static final int CODE_LENGTH = 6;
    static final int CONNECT_TIMEOUT_MILLIS = 3000;
    static final int READ_TIMEOUT_MILLIS = 5000;

    private static final String OK = "OK";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+?86)?(1[3-9]\\d{9})$");
    private static final Pattern SCENE_PATTERN = Pattern.compile("[A-Z0-9_-]{1,64}");
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final Client client;
    private final RedisSmsVerificationRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationProperties properties;
    private final Clock clock;

    public DefaultSmsVerificationService(
            Client client,
            RedisSmsVerificationRepository repository,
            VerificationCodeGenerator codeGenerator,
            VerificationProperties properties) {
        this(client, repository, codeGenerator, properties, Clock.systemUTC());
    }

    DefaultSmsVerificationService(
            Client client,
            RedisSmsVerificationRepository repository,
            VerificationCodeGenerator codeGenerator,
            VerificationProperties properties,
            Clock clock) {
        this.client = client;
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SmsVerificationSendResponse send(SmsVerificationSendRequest request) {
        ValidatedRequest validated = validate(request);
        VerificationProperties.SmsProperties smsProperties = properties.getSms();
        String code = generateCode();
        String reservationToken = UUID.randomUUID().toString();
        Instant reservedAt = clock.instant();

        RedisSmsVerificationRepository.Reservation reservation;
        try {
            reservation = repository.reserve(
                    validated.phoneNumber(),
                    validated.scene(),
                    reservationToken,
                    code,
                    reservedAt,
                    smsProperties.getExpireTime(),
                    smsProperties.getCooldown(),
                    smsProperties.getHourlyLimit(),
                    smsProperties.getDailyLimit());
        } catch (RuntimeException ex) {
            log.error("Failed to reserve SMS verification state, phoneNumber={}, scene={}, code={}",
                    validated.phoneNumber(), validated.scene(), code, ex);
            throw new VerificationStorageException("Failed to reserve SMS verification state", ex);
        }
        rejectIfNotReserved(reservation);

        String outId = UUID.randomUUID().toString();
        SendSmsVerifyCodeRequest aliyunRequest = createAliyunRequest(
                validated.phoneNumber(), code, smsProperties.getExpireTime(), outId);
        SendSmsVerifyCodeResponse aliyunResponse;
        try {
            aliyunResponse = client.sendSmsVerifyCodeWithOptions(aliyunRequest, runtimeOptions());
        } catch (Exception ex) {
            log.error("SMS verification delivery result is unknown, phoneNumber={}, scene={}, code={}",
                    validated.phoneNumber(), validated.scene(), code, ex);
            throw new SmsVerificationDeliveryException(
                    "SMS verification delivery result is unknown",
                    ex,
                    null);
        }

        SmsVerificationSendResponse response = mapResponse(aliyunResponse);
        if (!isSuccessful(response)) {
            rollback(validated, reservationToken, reservedAt, code);
            log.error("SMS verification delivery failed, phoneNumber={}, scene={}, code={}, response={}",
                    validated.phoneNumber(), validated.scene(), code, response);
            throw new SmsVerificationDeliveryException(
                    "Alibaba Cloud rejected the SMS verification request",
                    null,
                    response);
        }

        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody body = aliyunResponse.getBody();
        String requestId = body == null ? null : body.getRequestId();
        String bizId = body == null || body.getModel() == null
                ? null
                : body.getModel().getBizId();
        log.info("SMS verification sent, phoneNumber={}, scene={}, code={}, requestId={}, bizId={}",
                validated.phoneNumber(), validated.scene(), code, requestId, bizId);
        return response;
    }

    @Override
    public boolean verifyAndConsume(SmsVerificationVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String scene = normalizeScene(request.scene());
        String code = requireText(request.code(), "code").trim();

        RedisSmsVerificationRepository.VerificationResult result;
        try {
            result = repository.verifyAndConsume(
                    phoneNumber,
                    scene,
                    code,
                    properties.getSms().getMaxFailedAttempts());
        } catch (RuntimeException ex) {
            log.error("Failed to verify SMS code in Redis, phoneNumber={}, scene={}, code={}",
                    phoneNumber, scene, code, ex);
            throw new VerificationStorageException("Failed to verify SMS code", ex);
        }

        if (result.verified()) {
            log.info("SMS verification code consumed, phoneNumber={}, scene={}, code={}",
                    phoneNumber, scene, code);
        } else {
            log.warn("SMS verification code rejected, phoneNumber={}, scene={}, code={}, failedAttempts={}",
                    phoneNumber, scene, code, result.failedAttempts());
        }
        return result.verified();
    }

    private ValidatedRequest validate(SmsVerificationSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return new ValidatedRequest(
                normalizePhoneNumber(request.phoneNumber()),
                normalizeScene(request.scene()));
    }

    private String generateCode() {
        String code = codeGenerator.generate(CODE_LENGTH);
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalStateException("SMS verification code generator must return exactly 6 digits");
        }
        return code;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String value = requireText(phoneNumber, "phoneNumber").trim();
        Matcher matcher = PHONE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("phoneNumber must be a valid mainland China mobile number");
        }
        return matcher.group(1);
    }

    private String normalizeScene(VerificationScene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("scene must not be null");
        }
        String normalized = requireText(scene.code(), "scene code").trim().toUpperCase(Locale.ROOT);
        if (!SCENE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("scene code must match [A-Z0-9_-]{1,64}");
        }
        return normalized;
    }

    private void rejectIfNotReserved(RedisSmsVerificationRepository.Reservation reservation) {
        switch (reservation.status()) {
            case RESERVED -> {
                return;
            }
            case COOLDOWN -> throw new VerificationCooldownException(reservation.retryAfter());
            case HOURLY_LIMIT -> throw new VerificationRateLimitException(
                    VerificationRateLimitType.HOURLY, reservation.retryAfter());
            case DAILY_LIMIT -> throw new VerificationRateLimitException(
                    VerificationRateLimitType.DAILY, reservation.retryAfter());
            default -> throw new IllegalStateException(
                    "Unsupported SMS verification reservation status: " + reservation.status());
        }
    }

    private SendSmsVerifyCodeRequest createAliyunRequest(
            String phoneNumber,
            String code,
            Duration expireTime,
            String outId) {
        return new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phoneNumber)
                .setSignName(SIGN_NAME)
                .setTemplateCode(TEMPLATE_CODE)
                .setTemplateParam(templateParam(code, expireTime))
                .setOutId(outId)
                .setReturnVerifyCode(false)
                .setAutoRetry(0L);
    }

    private String templateParam(String code, Duration expireTime) {
        long minuteMillis = Duration.ofMinutes(1).toMillis();
        long minutes = (expireTime.toMillis() + minuteMillis - 1) / minuteMillis;
        return String.format(Locale.ROOT, "{\"code\":\"%s\",\"min\":\"%d\"}", code, minutes);
    }

    private RuntimeOptions runtimeOptions() {
        return new RuntimeOptions()
                .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                .setReadTimeout(READ_TIMEOUT_MILLIS)
                .setAutoretry(false)
                .setMaxAttempts(1);
    }

    private SmsVerificationSendResponse mapResponse(SendSmsVerifyCodeResponse response) {
        if (response == null) {
            return new SmsVerificationSendResponse(null, null, null);
        }
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody body = response.getBody();
        SmsVerificationSendResponseBody mappedBody = null;
        if (body != null) {
            SmsVerificationSendResponseModel model = null;
            if (body.getModel() != null) {
                model = new SmsVerificationSendResponseModel(
                        body.getModel().getRequestId(),
                        body.getModel().getOutId(),
                        body.getModel().getBizId());
            }
            mappedBody = new SmsVerificationSendResponseBody(
                    body.getSuccess(),
                    body.getCode(),
                    body.getMessage(),
                    body.getRequestId(),
                    body.getAccessDeniedDetail(),
                    model);
        }
        return new SmsVerificationSendResponse(
                response.getHeaders(),
                response.getStatusCode(),
                mappedBody);
    }

    private boolean isSuccessful(SmsVerificationSendResponse response) {
        return response.body() != null
                && Boolean.TRUE.equals(response.body().success())
                && OK.equals(response.body().code());
    }

    private void rollback(
            ValidatedRequest request,
            String reservationToken,
            Instant reservedAt,
            String code) {
        try {
            repository.rollback(
                    request.phoneNumber(),
                    request.scene(),
                    reservationToken,
                    reservedAt);
        } catch (RuntimeException rollbackException) {
            log.error("Failed to rollback SMS verification state, phoneNumber={}, scene={}, code={}",
                    request.phoneNumber(), request.scene(), code, rollbackException);
        }
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private record ValidatedRequest(
            String phoneNumber,
            String scene) {
    }
}
