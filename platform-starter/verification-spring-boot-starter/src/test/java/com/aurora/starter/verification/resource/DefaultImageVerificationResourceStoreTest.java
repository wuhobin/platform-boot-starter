package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultImageVerificationResourceStoreTest {

    @Test
    void registersOnlyTheFiveDefaultSliderBackgrounds() {
        DefaultImageVerificationResourceStore store =
                new DefaultImageVerificationResourceStore();

        assertThat(store.listResourcesByTypeAndTag(CaptchaTypeConstant.SLIDER, "default"))
                .hasSize(5)
                .allSatisfy(resource -> assertThat(resource.getType())
                        .isEqualToIgnoringCase("url"))
                .extracting(Resource::getData)
                .containsExactlyInAnyOrderElementsOf(
                        DefaultImageVerificationResourceStore.DEFAULT_SLIDER_BACKGROUNDS);
        assertThat(store.listResourcesByTypeAndTag(CaptchaTypeConstant.ROTATE, null))
                .isEmpty();
    }
}
