package com.aurora.starter.webmvc.security;

import com.aurora.starter.webmvc.exception.BizException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 使用平台凭据密钥保护服务端保存的凭据字段。
 *
 * <p>用途会作为 GCM 的附加认证数据，因此密文不能在不相关的凭据字段之间搬移。</p>
 * <p>密钥配置采用懒校验：应用启动时不强制要求密钥，第一次加密或解密时才校验。</p>
 */
public class PlatformCredentialCipher {

    private static final String VERSION = "v1";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SECRET_PROPERTY = "platform.webmvc.credential-secret-key";
    private static final String SECRET_ENVIRONMENT_VARIABLE = "PLATFORM_CREDENTIAL_SECRET_KEY";

    private final String encodedSecret;
    private volatile SecretKeySpec key;

    public PlatformCredentialCipher(String encodedSecret) {
        this.encodedSecret = encodedSecret;
    }

    public String encrypt(String purpose, String value) {
        requirePurpose(purpose);
        if (value == null) {
            throw new IllegalArgumentException("Credential value must not be null");
        }
        SecretKeySpec secretKey = requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new BizException("平台凭据加密失败", exception);
        }
    }

    public String decrypt(String purpose, String value) {
        requirePurpose(purpose);
        SecretKeySpec secretKey = requireKey();
        try {
            String[] parts = value.split(":", 3);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported credential ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            if (iv.length != IV_LENGTH) {
                throw new IllegalArgumentException("Invalid credential IV");
            }
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BizException("平台凭据解密失败，请检查平台凭据密钥是否一致", exception);
        }
    }

    private SecretKeySpec requireKey() {
        SecretKeySpec current = key;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = key;
            if (current == null) {
                current = new SecretKeySpec(decodeKey(encodedSecret), "AES");
                key = current;
            }
            return current;
        }
    }

    private static byte[] decodeKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException(
                    SECRET_PROPERTY + " must be configured (environment variable "
                            + SECRET_ENVIRONMENT_VARIABLE + ")");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedSecret.trim());
            if (decoded.length != KEY_LENGTH) {
                throw new IllegalStateException(
                        SECRET_PROPERTY + " must decode to exactly 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    SECRET_PROPERTY + " must be valid Base64", exception);
        }
    }

    private static void requirePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Credential purpose must not be blank");
        }
    }
}
