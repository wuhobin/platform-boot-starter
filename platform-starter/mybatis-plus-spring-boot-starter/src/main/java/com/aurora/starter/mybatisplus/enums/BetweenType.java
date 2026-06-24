package com.aurora.starter.mybatisplus.enums;

/**
 * 范围查询类型操作符.
 *
 * @author whb
 * @date 2026-4-11 11:19
 */
public enum BetweenType {

    /**
     * 左开右开( end > x > start ).
     */
    BOTH_NOT_CONTAIN,

    /**
     * 左闭右闭( end >= x >= start ).
     */
    BOTH_EQUAL,

    /**
     * 左闭右开( end >= x > start ).
     */
    ONLY_MIN_EQUAL,

    /**
     * 左开右闭( end > x >= start ).
     */
    ONLY_MAX_EQUAL;

}
