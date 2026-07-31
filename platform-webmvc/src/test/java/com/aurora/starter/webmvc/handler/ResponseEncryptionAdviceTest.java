package com.aurora.starter.webmvc.handler;

import com.aurora.starter.webmvc.annotation.EncryptResponse;
import com.aurora.starter.webmvc.domain.response.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseEncryptionAdviceTest {

    private static final byte[] KEY_BYTES = "0123456789abcdef0123456789abcdef"
            .getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ResponseEncryptionAdvice advice = new ResponseEncryptionAdvice(
            objectMapper,
            new SecretKeySpec(KEY_BYTES, "AES")
    );

    @Test
    void encryptsJsonDataAndLeavesOtherResultFieldsUnchanged() throws Exception {
        Result<?> result = Result
                .data(Map.<String, Object>of("name", "Aurora", "count", 2))
                .putExtra("page", 1);
        int code = result.getCode();
        String message = result.getMessage();

        Object returned = advice.beforeBodyWrite(
                result,
                returnType(MethodController.class, "encrypted"),
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null
        );

        assertThat(returned).isSameAs(result);
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getExtra()).containsEntry("page", 1);

        String encryptedData = (String) result.getData();
        assertThat(encryptedData).startsWith(ResponseEncryptionAdvice.VERSION + ".");
        JsonNode decrypted = objectMapper.readTree(decrypt(encryptedData));
        assertThat(decrypted.get("name").asText()).isEqualTo("Aurora");
        assertThat(decrypted.get("count").asInt()).isEqualTo(2);
    }

    @Test
    void usesFreshIvForEveryEncryption() throws Exception {
        Result<String> first = Result.data("payload");
        Result<String> second = Result.data("payload");

        encrypt(first);
        encrypt(second);

        String firstCiphertext = first.getData();
        String secondCiphertext = second.getData();
        assertThat(firstCiphertext).isNotEqualTo(secondCiphertext);
        assertThat(decrypt(firstCiphertext)).isEqualTo("\"payload\"");
        assertThat(decrypt(secondCiphertext)).isEqualTo("\"payload\"");
    }

    @Test
    void leavesNullDataUnchanged() throws Exception {
        Result<Void> result = Result.success();

        Object returned = advice.beforeBodyWrite(
                result,
                returnType(MethodController.class, "encrypted"),
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null
        );

        assertThat(returned).isSameAs(result);
        assertThat(result.getData()).isNull();
    }

    @Test
    void rejectsNonResultResponseBody() throws Exception {
        assertThatThrownBy(() -> advice.beforeBodyWrite(
                "payload",
                returnType(MethodController.class, "invalid"),
                MediaType.TEXT_PLAIN,
                MappingJackson2HttpMessageConverter.class,
                null,
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only supports Result");
    }

    @Test
    void detectsMethodTypeAndInterfaceAnnotations() throws Exception {
        assertThat(advice.supports(
                returnType(MethodController.class, "encrypted"),
                MappingJackson2HttpMessageConverter.class
        )).isTrue();
        assertThat(advice.supports(
                returnType(MethodController.class, "plain"),
                MappingJackson2HttpMessageConverter.class
        )).isFalse();
        assertThat(advice.supports(
                returnType(AnnotatedController.class, "response"),
                MappingJackson2HttpMessageConverter.class
        )).isTrue();
        assertThat(advice.supports(
                returnType(InterfaceController.class, "response"),
                MappingJackson2HttpMessageConverter.class
        )).isTrue();
    }

    private void encrypt(Result<?> result) throws Exception {
        advice.beforeBodyWrite(
                result,
                returnType(MethodController.class, "encrypted"),
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null
        );
    }

    private String decrypt(String encrypted) throws Exception {
        String[] parts = encrypted.split("\\.", 3);
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isEqualTo(ResponseEncryptionAdvice.VERSION);

        byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
        byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(KEY_BYTES, "AES"),
                new GCMParameterSpec(128, iv)
        );
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private static MethodParameter returnType(Class<?> controllerType, String methodName)
            throws NoSuchMethodException {
        Method method = controllerType.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    static class MethodController {

        @EncryptResponse
        public Result<String> encrypted() {
            return Result.data("payload");
        }

        public Result<String> plain() {
            return Result.data("payload");
        }

        @EncryptResponse
        public String invalid() {
            return "payload";
        }
    }

    @EncryptResponse
    static class AnnotatedController {

        public Result<String> response() {
            return Result.data("payload");
        }
    }

    interface AnnotatedApi {

        @EncryptResponse
        Result<String> response();
    }

    static class InterfaceController implements AnnotatedApi {

        @Override
        public Result<String> response() {
            return Result.data("payload");
        }
    }
}
