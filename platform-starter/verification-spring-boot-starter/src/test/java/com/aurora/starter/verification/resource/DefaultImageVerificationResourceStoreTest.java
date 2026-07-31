package com.aurora.starter.verification.resource;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultImageVerificationResourceStoreTest {

    @Test
    void registersFiveDefaultBackgroundsForEverySupportedType() {
        DefaultImageVerificationResourceStore store =
                new DefaultImageVerificationResourceStore();

        for (String type : List.of(
                CaptchaTypeConstant.SLIDER,
                CaptchaTypeConstant.ROTATE,
                CaptchaTypeConstant.CONCAT,
                CaptchaTypeConstant.WORD_IMAGE_CLICK)) {
            assertThat(store.listResourcesByTypeAndTag(type, "default"))
                    .hasSize(5)
                    .allSatisfy(resource -> assertThat(resource.getType())
                            .isEqualToIgnoringCase("url"))
                    .extracting(Resource::getData)
                    .containsExactlyInAnyOrderElementsOf(
                            DefaultImageVerificationResourceStore.DEFAULT_BACKGROUNDS);
        }
    }
}
