package com.aurora.starter.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * redis key 工具类.
 *
 * @author whb
 * @date 2026/6/18
 */
@Slf4j
@UtilityClass
public class RedisKeyUtil {

    public static final String DELIMITER = ":";

    /**
     * 生成redis key.
     *
     * @param fields 字段
     * @return String
     */
    public static String generate(final String... fields) {
        if (fields == null || fields.length < 1) {
            throw new IllegalArgumentException("fields can not be empty");
        }
        try {
            return Arrays.stream(fields).filter(StringUtils::isNotBlank).collect(Collectors.joining(DELIMITER));
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 分解redis key
     *
     * @param key redis key
     * @return
     */
    public static String[] split(String key) {
        return StringUtils.split(key, DELIMITER);
    }

}
