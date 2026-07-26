/*
 * All Rights Reserved: Copyright [2026] [wuhongbin (1289066006@qq.com)]
 * Open Source Agreement: Apache License, Version 2.0
 * For educational purposes only, commercial use shall comply with the author's copyright information.
 * The author does not guarantee or assume any responsibility for the risks of using software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aurora.starter.knife4j.config;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Swagger 自动配置类
 *
 * @Author wuhongbin (1289066006@qq.com)
 * @ProjectName platform-boot-starter
 * @ClassName com.aurora.starter.knife4j.config.SwaggerAutoConfiguration
 * @CreateTime 2026/7/14 - 10:52
 */

@AutoConfiguration(
        before = Knife4jAutoConfiguration.class,
        after = SpringDocConfigProperties.class)
@EnableConfigurationProperties(Knife4jProperties.class)
@ConditionalOnProperty(prefix = "knife4j", name = "enable", havingValue = "true")
@ConditionalOnProperty(
        prefix = "springdoc.api-docs",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SwaggerAutoConfiguration {

    @Primary
    @Bean("knife4jOpenApiCustomizer")
    @ConditionalOnBean(SpringDocConfigProperties.class)
    @ConditionalOnMissingBean(Knife4jOpenApiCustomizer.class)
    public MyKnife4jOpenApiCustomizer knife4jOpenApiCustomizer(
            Knife4jProperties properties, SpringDocConfigProperties docProperties) {
        return new MyKnife4jOpenApiCustomizer(properties, docProperties);
    }
}
