package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;

import java.util.List;

/**
 * 默认图片验证码远程背景资源。
 */
public final class DefaultImageVerificationResourceStore extends LocalMemoryResourceStore {

    private static final List<String> SUPPORTED_TYPES = List.of(
            CaptchaTypeConstant.SLIDER,
            CaptchaTypeConstant.ROTATE,
            CaptchaTypeConstant.CONCAT,
            CaptchaTypeConstant.WORD_IMAGE_CLICK);

    static final List<String> DEFAULT_BACKGROUNDS = List.of(
            "https://oss.wuhobin.top/captcha/8d4ca19573fe5084f2679e9af896debf.jpg?imageView2/1/w/600/h/300",
            "https://oss.wuhobin.top/captcha/ed68288125b316dfc1ebb54ec0a4a683.jpg?imageView2/1/w/600/h/300",
            "https://oss.wuhobin.top/captcha/bdb1b69f06a0afc06727428c1a8c51b1.jpg?imageView2/1/w/600/h/300",
            "https://oss.wuhobin.top/captcha/0812446b0907bce26c0462feebd4c5ba.jpeg?imageView2/1/w/600/h/300",
            "https://oss.wuhobin.top/captcha/956d07e4bdd8143f5ba06487b85ce8df.jpg?imageView2/1/w/600/h/300",
            "https://oss.wuhobin.top/captcha/53730776b67bd8c60f88c0d3075c021d.jpeg?imageView2/1/w/600/h/300");

    public DefaultImageVerificationResourceStore() {
        for (String type : SUPPORTED_TYPES) {
            for (String location : DEFAULT_BACKGROUNDS) {
                addResource(type, new Resource("url", location, "default"));
            }
        }
    }
}
