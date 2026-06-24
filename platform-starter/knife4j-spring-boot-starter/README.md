# knife4j-spring-boot-starter

> 基于 [Knife4j](https://doc.xiaominfo.com/) 4.5（OpenAPI 3 + Jakarta）的 Spring Boot 3 Starter，开箱即用。提供默认 `OpenAPI` Bean（标题/描述/版本/Contact/License 可配），原生 `knife4j.*` 与 `springdoc.*` 配置完全透传给官方 starter，不重复封装。

![JDK](https://img.shields.io/badge/JDK-21+-blue.svg)
![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.5.0-blue.svg)
![Knife4j](https://img.shields.io/badge/Knife4j-4.5.0-blue.svg)

---

## 一、引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> 该 starter 已传递引入 `knife4j-openapi3-jakarta-spring-boot-starter` 与 `springdoc-openapi-starter-webmvc-ui`，无需重复声明。

---

## 二、最小配置

```yaml
platform:
  knife4j:
    title: 用户中心 API

springdoc:
  group-configs:
    - group: default
      packages-to-scan: com.aurora.user.controller
```

启动后访问：

- Knife4j UI：<http://localhost:8080/doc.html>
- 原生 OpenAPI JSON：<http://localhost:8080/v3/api-docs>

---

## 三、自动装配内容

| Bean | 作用 | 说明 |
| --- | --- | --- |
| `OpenAPI auroraOpenApi` | 默认文档元信息（Info / Contact / License） | `@ConditionalOnMissingBean`，业务可整体覆盖 |

激活条件：

- classpath 存在 `io.swagger.v3.oas.models.OpenAPI`（即引入了本 starter 或 springdoc）
- `platform.knife4j.enable=true`（默认值）

---

## 四、`platform.knife4j` 完整配置项

| 配置 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `platform.knife4j.enable` | boolean | `true` | 总开关，关闭后不注入默认 `OpenAPI` Bean |
| `platform.knife4j.title` | String | `Aurora API` | 文档标题 |
| `platform.knife4j.description` | String | `""` | 文档描述 |
| `platform.knife4j.version` | String | `1.0.0` | API 版本 |
| `platform.knife4j.terms-of-service-url` | String | — | 服务条款 URL |
| `platform.knife4j.contact.name` | String | — | 联系人姓名 |
| `platform.knife4j.contact.email` | String | — | 联系人邮箱 |
| `platform.knife4j.contact.url` | String | — | 联系人主页 |
| `platform.knife4j.license.name` | String | — | 协议名（如 Apache 2.0） |
| `platform.knife4j.license.url` | String | — | 协议 URL |

`contact` / `license` 整组字段全为空时，不挂到 `OpenAPI.info`。

---

## 五、配合官方 Knife4j 配置

本 starter **不**封装官方命名空间，所有官方配置照常生效。常用场景：

```yaml
knife4j:
  # 生产环境隐藏文档
  production: false
  # 资源访问 Basic 鉴权
  basic:
    enable: false
    username: admin
    password: 123456
  # UI 增强
  setting:
    language: zh_cn
    enable-swagger-models: true
    enable-document-manage: true
```

完整列表参见官方文档：<https://doc.xiaominfo.com/docs/features/enhance>

---

## 六、分组与扫描（官方 springdoc）

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  group-configs:
    - group: 用户
      paths-to-match: /user/**
      packages-to-scan: com.aurora.user.controller
    - group: 订单
      paths-to-match: /order/**
      packages-to-scan: com.aurora.order.controller
```

---

## 七、覆盖默认 `OpenAPI` Bean

业务需要追加 `SecurityScheme`、`Server` 列表或自定义扩展，注入自己的 `OpenAPI` Bean 即可，自动覆盖 starter 默认实现：

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info().title("自定义标题").version("2.0.0"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
```

---

## 八、版本与依赖

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.0 |
| Knife4j (openapi3-jakarta) | 4.5.0 |
| springdoc-openapi | 由 knife4j 4.5.0 传递引入 |
