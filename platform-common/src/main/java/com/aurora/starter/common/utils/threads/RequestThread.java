package com.aurora.starter.common.utils.threads;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author whb
 * @date 2026/5/31
 */
@Slf4j
public class RequestThread {

    /**
     * 请求数据.
     */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置请求数据.
     *
     * @param dataMap 请求数据
     */
    public static void setData(final Map<String, Object> dataMap) {
        THREAD_LOCAL.set(dataMap);
    }

    /**
     * 获取请求数据.
     *
     * @return 请求数据
     */
    public static Map<String, Object> getData() {
        return THREAD_LOCAL.get();
    }

    /**
     * 添加请求数据.
     *
     * @param key   键
     * @param value 值
     */
    public static void addParam(final String key, final Object value) {
        Map<String, Object> map = THREAD_LOCAL.get();
        map.put(key, value);
        THREAD_LOCAL.set(map);
    }

    /**
     * 移除请求数据.
     *
     * @param key 键
     */
    public static void removeParam(final String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        map.remove(key);
        THREAD_LOCAL.set(map);
    }

    /**
     * 获取请求数据.
     *
     * @param key 键
     * @return 值
     */
    public static Object getParam(final String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        return map.get(key);
    }

    /**
     * 获取请求数据.
     *
     * @param key 键
     * @param <T> 泛型
     * @return 值
     */
    public static <T> T getValue(final String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        try {
            return (T) map.get(key);
        } catch (Exception e) {
            log.warn("从当前请求线程中获取【{}】的数据异常", key);
            return null;
        }
    }
    /**
     * 清除请求数据.
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }

}
