package com.aurora.starter.webmvc.security;

import com.aurora.starter.webmvc.exception.BizException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformCredentialCipherTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptsWithRandomNonceAndDecryptsForTheSamePurpose() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(SECRET);

        String first = cipher.encrypt("mail.auth-code", "credential");
        String second = cipher.encrypt("mail.auth-code", "credential");

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(cipher.decrypt("mail.auth-code", first)).isEqualTo("credential");
        assertThat(cipher.decrypt("mail.auth-code", second)).isEqualTo("credential");
    }

    @Test
    void rejectsCiphertextFromAnotherPurpose() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(SECRET);
        String encrypted = cipher.encrypt("mail.auth-code", "credential");

        assertThatThrownBy(() -> cipher.decrypt("monitor.ssh-password", encrypted))
                .isInstanceOf(BizException.class);
    }

    @Test
    void validatesMissingOrInvalidKeysOnFirstUse() {
        PlatformCredentialCipher missing = new PlatformCredentialCipher("");
        assertThatThrownBy(() -> missing.encrypt("purpose", "credential"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credential-secret-key");

        PlatformCredentialCipher invalid = new PlatformCredentialCipher("not-base64!");
        assertThatThrownBy(() -> invalid.encrypt("purpose", "credential"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");

        PlatformCredentialCipher shortKey = new PlatformCredentialCipher(
                Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> shortKey.encrypt("purpose", "credential"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 32 bytes");
    }
}
