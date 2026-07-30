package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;

import java.util.List;

/**
 * 默认滑块验证码远程背景资源。
 */
public final class DefaultImageVerificationResourceStore extends LocalMemoryResourceStore {

    static final List<String> DEFAULT_SLIDER_BACKGROUNDS = List.of(
            "https://oss.wuhobin.top/base/20260418/20260418165311_949166a0.png",
            "https://oss.wuhobin.top/base/20260418/20260418165808_928ffbe5.png",
            "https://oss.wuhobin.top/base/20260418/20260418170001_567d69ad.png",
            "https://oss.wuhobin.top/base/20260418/20260418170105_613e5cae.png",
            "https://oss.wuhobin.top/base/20260418/20260418170445_3e29d1e0.png");

    public DefaultImageVerificationResourceStore() {
        for (String location : DEFAULT_SLIDER_BACKGROUNDS) {
            addResource(
                    CaptchaTypeConstant.SLIDER,
                    new Resource("url", location, "default"));
        }
    }
}
