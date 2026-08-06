package com.aurora.starter.webmvc.handler;

import com.aurora.starter.webmvc.annotation.EncryptResponse;
import com.aurora.starter.webmvc.domain.response.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 对标注了 {@link EncryptResponse} 的接口响应进行加密。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class ResponseEncryptionAdvice implements ResponseBodyAdvice<Object> {

    static final String VERSION = "v1";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String SECRET_PROPERTY = "platform.webmvc.response-secret-key";
    private static final String SECRET_ENVIRONMENT_VARIABLE = "PLATFORM_RESPONSE_SECRET_KEY";

    private final ObjectMapper objectMapper;

    private final String encodedSecretKey;

    private final SecretKey fixedSecretKey;

    private volatile SecretKey parsedSecretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public ResponseEncryptionAdvice(ObjectMapper objectMapper, SecretKey secretKey) {
        this.objectMapper = objectMapper;
        this.encodedSecretKey = null;
        this.fixedSecretKey = secretKey;
    }

    public ResponseEncryptionAdvice(ObjectMapper objectMapper, String encodedSecretKey) {
        this.objectMapper = objectMapper;
        this.encodedSecretKey = encodedSecretKey;
        this.fixedSecretKey = null;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return hasEncryptResponseAnnotation(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof Result<?> result)) {
            throw new IllegalStateException("@EncryptResponse only supports Result response bodies");
        }
        if (result.getData() == null) {
            return result;
        }

        setEncryptedData(result, encrypt(serialize(result.getData())));
        return result;
    }

    private byte[] serialize(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize response data for encryption", exception);
        }
    }

    private String encrypt(byte[] plaintext) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireSecretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            return VERSION + "."
                    + BASE64_URL_ENCODER.encodeToString(iv) + "."
                    + BASE64_URL_ENCODER.encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt response data", exception);
        }
    }

    private SecretKey requireSecretKey() {
        if (fixedSecretKey != null) {
            return fixedSecretKey;
        }
        SecretKey current = parsedSecretKey;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = parsedSecretKey;
            if (current == null) {
                current = new SecretKeySpec(decodeKey(encodedSecretKey), "AES");
                parsedSecretKey = current;
            }
            return current;
        }
    }

    private static byte[] decodeKey(String encodedSecretKey) {
        if (encodedSecretKey == null || encodedSecretKey.isBlank()) {
            throw new IllegalStateException(
                    SECRET_PROPERTY + " must be configured (environment variable "
                            + SECRET_ENVIRONMENT_VARIABLE + ")");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedSecretKey.trim());
            if (decoded.length != 32) {
                throw new IllegalStateException(
                        SECRET_PROPERTY + " must decode to exactly 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    SECRET_PROPERTY + " must be valid Base64", exception);
        }
    }

    private static boolean hasEncryptResponseAnnotation(MethodParameter returnType) {
        Class<?> containingClass = returnType.getContainingClass();
        if (AnnotatedElementUtils.hasAnnotation(containingClass, EncryptResponse.class)) {
            return true;
        }

        Method method = returnType.getMethod();
        if (method == null) {
            return false;
        }

        Method specificMethod = ClassUtils.getMostSpecificMethod(method, containingClass);
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        if (AnnotatedElementUtils.hasAnnotation(bridgedMethod, EncryptResponse.class)) {
            return true;
        }

        for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(containingClass)) {
            Method interfaceMethod = ReflectionUtils.findMethod(
                    interfaceType, method.getName(), method.getParameterTypes());
            if (interfaceMethod != null
                    && AnnotatedElementUtils.hasAnnotation(interfaceMethod, EncryptResponse.class)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void setEncryptedData(Result<?> result, String encryptedData) {
        ((Result<Object>) result).setData(encryptedData);
    }
}
