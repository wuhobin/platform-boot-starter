package com.aurora.starter.verification.mail;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationDeliveryException;
import com.aurora.starter.verification.exception.VerificationStorageException;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.scene.VerificationScene;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 默认邮件验证码服务。
 */
@Slf4j
public class DefaultMailVerificationService implements MailVerificationService {

    private static final String CODE_PLACEHOLDER = "{code}";
    private static final String EXPIRE_MINUTES_PLACEHOLDER = "{expireMinutes}";
    private static final Pattern SCENE_PATTERN = Pattern.compile("[A-Z0-9_-]{1,64}");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");

    private final JavaMailSender mailSender;
    private final RedisMailVerificationRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationProperties properties;
    private final String from;

    public DefaultMailVerificationService(
            JavaMailSender mailSender,
            RedisMailVerificationRepository repository,
            VerificationCodeGenerator codeGenerator,
            VerificationProperties properties,
            String from) {
        this.mailSender = mailSender;
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.properties = properties;
        this.from = from;
        validateSingleEmail(from);
    }

    @Override
    public void send(MailVerificationSendRequest request) {
        ValidatedSendRequest validated = validate(request);
        VerificationProperties.MailProperties mailProperties = properties.getMail();
        String cooldownToken = UUID.randomUUID().toString();

        boolean acquired;
        try {
            acquired = repository.acquireCooldown(
                    validated.email(), validated.scene(), cooldownToken, mailProperties.getCooldown());
        } catch (RuntimeException ex) {
            log.error("Failed to acquire verification cooldown, email={}, scene={}",
                    validated.email(), validated.scene(), ex);
            throw new VerificationStorageException("Failed to acquire verification cooldown", ex);
        }

        if (!acquired) {
            Duration retryAfter;
            try {
                retryAfter = repository.getCooldownRemaining(validated.email(), validated.scene());
            } catch (RuntimeException ex) {
                log.error("Failed to read verification cooldown, email={}, scene={}",
                        validated.email(), validated.scene(), ex);
                throw new VerificationStorageException("Failed to read verification cooldown", ex);
            }
            log.warn("Verification mail cooldown hit, email={}, scene={}, retryAfterMs={}",
                    validated.email(), validated.scene(), retryAfter.toMillis());
            throw new VerificationCooldownException(retryAfter);
        }

        String code = codeGenerator.generate(mailProperties.getCodeLength());
        String content = renderContent(validated.content(), code, mailProperties.getExpireTime());
        try {
            deliver(validated, content);
        } catch (VerificationDeliveryException ex) {
            releaseCooldown(validated.email(), validated.scene(), cooldownToken, code);
            log.error("Verification mail delivery failed, email={}, scene={}, code={}",
                    validated.email(), validated.scene(), code, ex);
            throw ex;
        }

        try {
            boolean stored = repository.storeCodeIfCooldownOwned(
                    validated.email(),
                    validated.scene(),
                    cooldownToken,
                    code,
                    mailProperties.getExpireTime());
            if (!stored) {
                throw new VerificationStorageException(
                        "Verification cooldown ownership expired before the code was stored");
            }
        } catch (VerificationStorageException ex) {
            releaseCooldown(validated.email(), validated.scene(), cooldownToken, code);
            log.error("Failed to store verification code, email={}, scene={}, code={}",
                    validated.email(), validated.scene(), code, ex);
            throw ex;
        } catch (RuntimeException ex) {
            releaseCooldown(validated.email(), validated.scene(), cooldownToken, code);
            log.error("Failed to store verification code, email={}, scene={}, code={}",
                    validated.email(), validated.scene(), code, ex);
            throw new VerificationStorageException("Failed to store verification code", ex);
        }

        log.info("Verification mail sent, email={}, scene={}, code={}",
                validated.email(), validated.scene(), code);
    }

    @Override
    public boolean verifyAndConsume(MailVerificationVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String email = normalizeEmail(request.email());
        String scene = normalizeScene(request.scene());
        String code = requireText(request.code(), "code");

        VerificationProperties.MailProperties mailProperties = properties.getMail();
        if (code.length() != mailProperties.getCodeLength() || !DIGITS_PATTERN.matcher(code).matches()) {
            log.warn("Verification code rejected, email={}, scene={}, code={}", email, scene, code);
            return false;
        }

        boolean verified;
        try {
            verified = repository.verifyAndConsume(email, scene, code);
        } catch (RuntimeException ex) {
            log.error("Failed to verify code in Redis, email={}, scene={}, code={}",
                    email, scene, code, ex);
            throw new VerificationStorageException("Failed to verify code", ex);
        }

        if (verified) {
            log.info("Verification code consumed, email={}, scene={}, code={}", email, scene, code);
        } else {
            log.warn("Verification code rejected, email={}, scene={}, code={}", email, scene, code);
        }
        return verified;
    }

    private ValidatedSendRequest validate(MailVerificationSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String email = normalizeEmail(request.email());
        String scene = normalizeScene(request.scene());
        String subject = requireText(request.subject(), "subject");
        String content = requireText(request.content(), "content");
        if (!content.contains(CODE_PLACEHOLDER)) {
            throw new IllegalArgumentException("content must contain {code}");
        }
        if (request.contentType() == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        return new ValidatedSendRequest(email, scene, subject, content, request.contentType());
    }

    private void deliver(ValidatedSendRequest request, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            VerificationProperties.MailProperties mailProperties = properties.getMail();
            if (StringUtils.hasText(mailProperties.getFromName())) {
                helper.setFrom(from, mailProperties.getFromName().trim());
            } else {
                helper.setFrom(from);
            }
            helper.setTo(request.email());
            helper.setSubject(request.subject());
            helper.setText(content, request.contentType() == MailContentType.HTML);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            throw new VerificationDeliveryException("Failed to send verification mail", ex);
        }
    }

    private void releaseCooldown(String email, String scene, String token, String code) {
        try {
            repository.releaseCooldown(email, scene, token);
        } catch (RuntimeException releaseException) {
            log.error("Failed to release verification cooldown, email={}, scene={}, code={}",
                    email, scene, code, releaseException);
        }
    }

    private String renderContent(String template, String code, Duration expireTime) {
        long expireMinutes = (expireTime.toMillis() + Duration.ofMinutes(1).toMillis() - 1)
                / Duration.ofMinutes(1).toMillis();
        return template.replace(CODE_PLACEHOLDER, code)
                .replace(EXPIRE_MINUTES_PLACEHOLDER, Long.toString(expireMinutes));
    }

    private String normalizeEmail(String email) {
        String normalized = requireText(email, "email").trim().toLowerCase(Locale.ROOT);
        validateSingleEmail(normalized);
        return normalized;
    }

    private void validateSingleEmail(String email) {
        String value = requireText(email, "email").trim();
        try {
            InternetAddress[] addresses = InternetAddress.parse(value, true);
            if (addresses.length != 1 || !value.equalsIgnoreCase(addresses[0].getAddress())) {
                throw new IllegalArgumentException("email must contain exactly one address");
            }
            addresses[0].validate();
        } catch (MessagingException ex) {
            throw new IllegalArgumentException("invalid email address", ex);
        }
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

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private record ValidatedSendRequest(
            String email,
            String scene,
            String subject,
            String content,
            MailContentType contentType) {
    }
}
