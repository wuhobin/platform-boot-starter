package com.aurora.starter.verification.mail;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationDeliveryException;
import com.aurora.starter.verification.exception.VerificationStorageException;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMailVerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisMailVerificationRepository repository;

    @Mock
    private VerificationCodeGenerator codeGenerator;

    private VerificationProperties properties;
    private DefaultMailVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new VerificationProperties();
        properties.getMail().setEnabled(true);
        properties.getMail().setFrom("no-reply@example.com");
        properties.getMail().setFromName("Aurora");
        service = new DefaultMailVerificationService(
                mailSender, repository, codeGenerator, properties, "no-reply@example.com");
    }

    @Test
    void sendsRenderedHtmlAndStoresCodeAfterDelivery() throws Exception {
        MimeMessage message = mimeMessage();
        when(repository.acquireCooldown(
                eq("user@example.com"), eq("REGISTER"), anyString(), eq(Duration.ofSeconds(60))))
                .thenReturn(true);
        when(codeGenerator.generate(6)).thenReturn("012345");
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(repository.storeCodeIfCooldownOwned(
                eq("user@example.com"), eq("REGISTER"), anyString(),
                eq("012345"), eq(Duration.ofMinutes(5))))
                .thenReturn(true);

        service.send(new MailVerificationSendRequest(
                " User@Example.com ",
                CommonVerificationScene.REGISTER,
                "Register",
                "<b>{code}</b> expires in {expireMinutes} minutes",
                MailContentType.HTML));
        message.saveChanges();

        verify(mailSender).send(message);
        verify(repository).storeCodeIfCooldownOwned(
                eq("user@example.com"), eq("REGISTER"), anyString(),
                eq("012345"), eq(Duration.ofMinutes(5)));
        assertThat(message.getSubject()).isEqualTo("Register");
        assertThat(message.getAllRecipients()).extracting(Object::toString)
                .containsExactly("user@example.com");
        assertThat(message.getContentType()).startsWith("text/html");
        assertThat(message.getContent().toString())
                .contains("<b>012345</b> expires in 5 minutes");
    }

    @Test
    void rejectsTemplateWithoutCodeBeforeAcquiringCooldown() {
        assertThatThrownBy(() -> service.send(new MailVerificationSendRequest(
                "user@example.com",
                CommonVerificationScene.LOGIN,
                "Login",
                "No placeholder",
                MailContentType.TEXT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{code}");

        verify(repository, never()).acquireCooldown(anyString(), anyString(), anyString(), any());
    }

    @Test
    void reportsCooldownWithRetryAfter() {
        when(repository.acquireCooldown(anyString(), anyString(), anyString(), any()))
                .thenReturn(false);
        when(repository.getCooldownRemaining("user@example.com", "LOGIN"))
                .thenReturn(Duration.ofSeconds(20));

        assertThatThrownBy(() -> service.send(textRequest()))
                .isInstanceOf(VerificationCooldownException.class)
                .extracting("retryAfter")
                .isEqualTo(Duration.ofSeconds(20));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void releasesCooldownWhenDeliveryFails() {
        MimeMessage message = mimeMessage();
        when(repository.acquireCooldown(anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
        when(codeGenerator.generate(6)).thenReturn("123456");
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(message);

        assertThatThrownBy(() -> service.send(textRequest()))
                .isInstanceOf(VerificationDeliveryException.class)
                .hasCauseInstanceOf(MailSendException.class);

        verify(repository).releaseCooldown(eq("user@example.com"), eq("LOGIN"), anyString());
        verify(repository, never()).storeCodeIfCooldownOwned(
                anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void releasesCooldownWhenRedisStoreFailsAfterDelivery() {
        MimeMessage message = mimeMessage();
        when(repository.acquireCooldown(anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
        when(codeGenerator.generate(6)).thenReturn("123456");
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(repository.storeCodeIfCooldownOwned(
                anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> service.send(textRequest()))
                .isInstanceOf(VerificationStorageException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(mailSender).send(message);
        verify(repository).releaseCooldown(eq("user@example.com"), eq("LOGIN"), anyString());
    }

    @Test
    void verifiesAndConsumesCode() {
        when(repository.verifyAndConsume("user@example.com", "RESET_PASSWORD", "654321"))
                .thenReturn(true);

        boolean result = service.verifyAndConsume(new MailVerificationVerifyRequest(
                "User@Example.com",
                CommonVerificationScene.RESET_PASSWORD,
                "654321"));

        assertThat(result).isTrue();
    }

    @Test
    void rejectsMalformedCodeWithoutReadingRedis() {
        boolean result = service.verifyAndConsume(new MailVerificationVerifyRequest(
                "user@example.com",
                CommonVerificationScene.LOGIN,
                "12A"));

        assertThat(result).isFalse();
        verify(repository, never()).verifyAndConsume(anyString(), anyString(), anyString());
    }

    private MailVerificationSendRequest textRequest() {
        return new MailVerificationSendRequest(
                "user@example.com",
                CommonVerificationScene.LOGIN,
                "Login",
                "Your code is {code}",
                MailContentType.TEXT);
    }

    private MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
