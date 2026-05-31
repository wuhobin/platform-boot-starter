package com.aurora.starter.mybatisplus.interceptor;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

/**
 * 对拦截器做扩展.
 *
 * @author whb
 * @date 2026/5/31
 */
@FunctionalInterface
public interface CustomInterceptor {

    void customInterceptor(MybatisPlusInterceptor mybatisPlusInterceptor);
}
