package com.aurora.starter.security.context;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class SecurityUtilsTest {

    @Test
    void returnsLoginIdAsIntThroughFacade() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsInt).thenReturn(7);

            assertThat(SecurityUtils.getLoginIdAsInt()).isEqualTo(7);
            stpUtil.verify(StpUtil::getLoginIdAsInt);
        }
    }

    @Test
    void logsOutSpecifiedTokenThroughFacade() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            SecurityUtils.logoutByTokenValue("token-123");

            stpUtil.verify(() -> StpUtil.logoutByTokenValue("token-123"));
        }
    }
}
