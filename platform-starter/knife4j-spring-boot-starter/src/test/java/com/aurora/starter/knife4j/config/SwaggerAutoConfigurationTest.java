package com.aurora.starter.knife4j.config;

import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SwaggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SwaggerAutoConfiguration.class))
            .withBean(TestSpringDocConfigProperties.class, TestSpringDocConfigProperties::new);

    private final WebApplicationContextRunner springDocContextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            WebMvcAutoConfiguration.class,
                            SpringDocConfiguration.class,
                            SpringDocConfigProperties.class,
                            SwaggerAutoConfiguration.class));

    @Test
    void backsOffWhenKnife4jIsDisabled() {
        contextRunner
                .withPropertyValues("knife4j.enable=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SwaggerAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(MyKnife4jOpenApiCustomizer.class);
                });
    }

    @Test
    void registersCustomizerWhenKnife4jAndApiDocsAreEnabled() {
        contextRunner
                .withPropertyValues("knife4j.enable=true", "springdoc.api-docs.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MyKnife4jOpenApiCustomizer.class));
    }

    @Test
    void skipsCustomizerWhenSpringDocApiDocsAreDisabled() {
        contextRunner
                .withPropertyValues("knife4j.enable=true", "springdoc.api-docs.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SwaggerAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(MyKnife4jOpenApiCustomizer.class);
                });
    }

    @Test
    void backsOffWhenApplicationProvidesCustomizer() {
        Knife4jOpenApiCustomizer customizer = mock(Knife4jOpenApiCustomizer.class);

        contextRunner
                .withPropertyValues("knife4j.enable=true")
                .withBean(Knife4jOpenApiCustomizer.class, () -> customizer)
                .run(context -> {
                    assertThat(context).hasSingleBean(Knife4jOpenApiCustomizer.class);
                    assertThat(context).doesNotHaveBean(MyKnife4jOpenApiCustomizer.class);
                });
    }

    @Test
    void keepsOfficialSpringDocPropertiesAvailableInWebApplication() {
        springDocContextRunner
                .withPropertyValues(
                        "knife4j.enable=true",
                        "springdoc.api-docs.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringDocConfigProperties.class);
                    assertThat(context).hasSingleBean(MyKnife4jOpenApiCustomizer.class);
                });
    }

    private static final class TestSpringDocConfigProperties extends SpringDocConfigProperties {
    }
}
