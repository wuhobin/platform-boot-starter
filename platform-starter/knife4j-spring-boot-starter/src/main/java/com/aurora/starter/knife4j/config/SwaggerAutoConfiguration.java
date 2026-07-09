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

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Swagger 自动配置类
 *
 * @Author wuhongbin (1289066006@qq.com)
 * @ProjectName platform-boot-starter
 * @ClassName com.aurora.starter.knife4j.config.SwaggerAutoConfiguration
 * @CreateTime 2026/7/14 - 10:52
 */

@Configuration
public class SwaggerAutoConfiguration {

    private final Knife4jProperties properties;

    public SwaggerAutoConfiguration(Knife4jProperties properties) {
        this.properties = properties;
    }

    @Primary
    @Bean("knife4jOpenApiCustomizer")
    public MyKnife4jOpenApiCustomizer knife4jOpenApiCustomizer(SpringDocConfigProperties docProperties) {
        return new MyKnife4jOpenApiCustomizer(this.properties, docProperties);
    }
}
