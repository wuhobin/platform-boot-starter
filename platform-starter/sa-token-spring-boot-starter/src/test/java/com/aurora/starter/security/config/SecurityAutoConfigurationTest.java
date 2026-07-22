package com.aurora.starter.security.config;

import com.aurora.starter.security.spi.PermissionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAutoConfigurationTest {

    @Test
    void startupSummaryUsesPlatformLineSeparators() {
        ObjectProvider<PermissionProvider> permissionProvider = new StaticListableBeanFactory()
                .getBeanProvider(PermissionProvider.class);
        SecurityAutoConfiguration configuration = new SecurityAutoConfiguration(
                new SecurityProperties(), permissionProvider, List.of());

        assertThat(configuration.buildStartupSummary())
                .contains(System.lineSeparator() + "    Token")
                .contains(System.lineSeparator() + "      ├─ login")
                .doesNotContain("%n");
    }
}
