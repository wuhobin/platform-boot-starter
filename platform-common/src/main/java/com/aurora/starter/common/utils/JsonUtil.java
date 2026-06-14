package com.aurora.starter.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

/**
 * Jackson工具类.
 *
 * @author xht.
 */
@Slf4j
public class JsonUtil {

    /**
     * 将对象序列化成json字符串.
     *
     * @param value javaBean
     * @param <T>   T 泛型标记
     * @return jsonString json字符串
     */
    public static <T> String toJson(final T value) {
        try {
            return getInstance().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将对象序列化成 json byte 数组.
     *
     * @param object javaBean
     * @return jsonString json字符串
     */
    public static byte[] toJsonAsBytes(final Object object) {
        try {
            return getInstance().writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成ArrayList集合.
     *
     * @param content   content
     * @param <T>       T 泛型标记
     * @return Bean
     */
    public static <T> List<T> parseList(final String content, final Class<T> clazz) {
        try {
            CollectionType collectionType = getInstance().getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            return getInstance().readValue(content, collectionType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化带泛型参数对象.
     *
     * @param content   content
     * @param <T>       T 泛型标记
     * @param <C>       T 类的泛型标记
     * @return Bean
     */
    public static <T, C> T parseGeneric(final String content, final Class<T> superClass, final Class<C> clazz) {
        try {
            JavaType javaType = getInstance().getTypeFactory().constructType(clazz);
            JavaType generalizedType = getInstance().getTypeFactory().constructParametricType(superClass, javaType);
            return getInstance().readValue(content, generalizedType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化为包含List泛型的对象.
     * 例如：解析 NrjResponse<List<DyShopResponse>> 类型
     *
     * @param content        JSON字符串
     * @param wrapperClass   外层包装类，如 NrjResponse.class
     * @param elementClass   List元素类型，如 DyShopResponse.class
     * @param <W>            外层包装类型
     * @param <E>            List元素类型
     * @return 解析后的对象
     */
    public static <W, E> W parseWithListGeneric(final String content, final Class<W> wrapperClass, final Class<E> elementClass) {
        try {
            JavaType listType = getInstance().getTypeFactory().constructCollectionType(List.class, elementClass);
            JavaType wrapperType = getInstance().getTypeFactory().constructParametricType(wrapperClass, listType);
            return getInstance().readValue(content, wrapperType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成对象.
     *
     * @param content   content
     * @param valueType class
     * @param <T>       T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final String content, final Class<T> valueType) {
        try {
            return getInstance().readValue(content, valueType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成对象.
     *
     * @param content       content
     * @param typeReference 泛型类型
     * @param <T>           T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final String content, final TypeReference<T> typeReference) {
        try {
            return getInstance().readValue(content, typeReference);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json byte 数组反序列化成对象.
     *
     * @param bytes     json bytes
     * @param valueType class
     * @param <T>       T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final byte[] bytes, final Class<T> valueType) {
        try {
            return getInstance().readValue(bytes, valueType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成对象.
     *
     * @param bytes         bytes
     * @param typeReference 泛型类型
     * @param <T>           T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final byte[] bytes, final TypeReference<T> typeReference) {
        try {
            return getInstance().readValue(bytes, typeReference);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成对象.
     *
     * @param in        InputStream
     * @param valueType class
     * @param <T>       T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final InputStream in, final Class<T> valueType) {
        try {
            return getInstance().readValue(in, valueType);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json反序列化成对象.
     *
     * @param in            InputStream
     * @param typeReference 泛型类型
     * @param <T>           T 泛型标记
     * @return Bean
     */
    public static <T> T parse(final InputStream in, final TypeReference<T> typeReference) {
        try {
            return getInstance().readValue(in, typeReference);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 转Map.
     *
     * @param content 内容
     * @return Map
     */
    public static Map<String, Object> toMap(final String content) {
        try {
            return getInstance().readValue(content, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 转对象.
     *
     * @param fromValue   map
     * @param toValueType 类实例
     * @param <T>         类型
     * @return T
     */
    public static <T> T toPojo(final Map fromValue, final Class<T> toValueType) {
        return getInstance().convertValue(fromValue, toValueType);
    }

    /**
     * 将json字符串转成 JsonNode.
     *
     * @param jsonString jsonString
     * @return jsonString json字符串
     */
    public static JsonNode readTree(final String jsonString) {
        try {
            return getInstance().readTree(jsonString);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json字符串转成 JsonNode.
     *
     * @param in InputStream
     * @return jsonString json字符串
     */
    public static JsonNode readTree(final InputStream in) {
        try {
            return getInstance().readTree(in);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json字符串转成 JsonNode.
     *
     * @param content content
     * @return jsonString json字符串
     */
    public static JsonNode readTree(final byte[] content) {
        try {
            return getInstance().readTree(content);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    /**
     * 将json字符串转成 JsonNode.
     *
     * @param jsonParser JsonParser
     * @return jsonString json字符串
     */
    public static JsonNode readTree(final JsonParser jsonParser) {
        try {
            return getInstance().readTree(jsonParser);
        } catch (IOException e) {
            throw ExceptionUtil.unchecked(e);
        }
    }

    public static ObjectMapper getInstance() {
        return JacksonHolder.INSTANCE;
    }

    /**
     * JacksonHolder.
     */
    private static class JacksonHolder {
        private static final ObjectMapper INSTANCE = new JacksonObjectMapper();
    }

    /**
     * JacksonObjectMapper.
     */
    public static class JacksonObjectMapper extends ObjectMapper {
        private static final long serialVersionUID = 4288193147502386170L;

        public JacksonObjectMapper() {
            super();
            super.setLocale(Locale.CHINA);
            super.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            super.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA));
            super.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            super.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            super.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            super.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            super.registerModule(new JavaTimeModule());
        }

    }

}
