package com.aurora.starter.xlock.model;

import lombok.*;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 锁key信息.
 *
 * @author whb
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public final class KeyInfo {

    /**
     * 锁key前缀.
     */
    public static final String KEY_PREFIX = "xlock:key:";

    /**
     * 空key.
     */
    public static final String EMPTY_KEY = "EMPTY_KEY";

    private String prefix;

    private String[] keys;

    private long leaseTime = -1L;

    private long waitTime = -1L;

    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private String errorMessage;

    private boolean disableLog = false;

    /**
     * 获取key信息.
     *
     * @return key信息
     */
    public String getKey() {
        return KEY_PREFIX + prefix + "-" + StringUtils.arrayToDelimitedString(getKeys(), "-");
    }

    public List<String> getRealKeys() {
        return Arrays.stream(getKeys()).map(k -> KEY_PREFIX + prefix + "-" + k).collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return keys == null || keys.length == 0;
    }
}
