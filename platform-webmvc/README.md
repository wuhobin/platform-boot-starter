# platform-webmvc

> Spring Boot 3 Web MVC 通用件。沉淀统一响应体 `Result<T>`、业务异常 `BizException`、全局异常处理器、TraceId 透传和请求日志。通过 Spring Boot 自动装配生效，下游引入依赖即可使用，无需追加组件扫描包。

![JDK](https://img.shields.io/badge/JDK-21+-blue.svg)
![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.5.0-blue.svg)

---

## 一、引入依赖

```xml
<dependency>
    <groupId>io.github.wuhobin</groupId>
    <artifactId>platform-webmvc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

自动装配会注册 `GlobalExceptionHandler`、`SaTokenExceptionHandler`、`TraceIdFilter`、`RequestLogFilter`、`PlatformCredentialCipher` 和 `ResponseEncryptionAdvice`。响应加密只有在接口标注 `@EncryptResponse` 时才生效；两个密钥都在对应功能第一次使用时懒校验。业务工程声明同类型 Bean 时，平台默认实现会自动退让。

---

## 二、统一响应体 `Result<T>`

```java
@GetMapping("/users/{id}")
public Result<User> get(@PathVariable Long id) {
    return Result.data(userService.findById(id));
}

@PostMapping
public Result<Void> create(@RequestBody @Valid UserCreateDTO dto) {
    userService.create(dto);
    return Result.success();                 // code=200, message="success"
}

@GetMapping("/exists/{id}")
public Result<Boolean> exists(@PathVariable Long id) {
    return Result.result(userService.exists(id));   // true → success(); false → error()
}
```

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 业务码，200=成功，4xx/5xx=错误（HTTP 风格） |
| `message` | String | 提示信息 |
| `data` | T | 业务数据，失败响应通常为 null |
| `traceId` | String | 构造时从 MDC 取，未配置 trace 时为 null |
| `extra` | Map | 可选扩展信息 |

**静态工厂**：

| 方法 | 用途 |
| --- | --- |
| `Result.success()` | 成功，code=200，message="success"，data=null |
| `Result.success(message)` | 成功，自定义提示 |
| `Result.data(data)` | 成功 + 数据 |
| `Result.success(message, data)` | 成功 + 自定义提示 + 数据 |
| `Result.result(boolean)` | boolean 快捷分支：true → success()，false → error() |
| `Result.error()` | 失败，code=500，message="操作失败" |
| `Result.error(message)` | 失败，自定义提示（code=500） |
| `Result.error(bizCode)` | 失败，依据 `BizCode` 取 code 与默认 message |
| `Result.error(bizCode, message)` | 失败，依据 `BizCode` 取 code，自定义 message |
| `Result.error(code, message)` | 失败，完全自定义 code 与 message |

实例方法 `result.ok()` 返回 `code == 200`，便于业务侧链式判断。

> **HTTP 状态码**统一返回 200，业务结果由 `code` 字段承载。如需 4xx/5xx 走 HTTP 状态码，业务侧自行写 `@ExceptionHandler` 覆盖。
>
> **OpenAPI 注解**：`Result` 与字段已用 `@Schema` 标注，knife4j/Swagger UI 自动渲染。

---

## 三、接口返回加密

响应加密由 `@EncryptResponse` opt-in。标注了该注解的 Controller 方法或类会使用 AES-256-GCM 加密 `Result.data`；未标注的接口保持原响应格式：

```yaml
platform:
  webmvc:
    response-secret-key: ${PLATFORM_RESPONSE_SECRET_KEY:} # Base64 编码的 32 字节密钥
    credential-secret-key: ${PLATFORM_CREDENTIAL_SECRET_KEY:} # Base64 编码的 32 字节密钥
```

可以使用 OpenSSL 生成密钥：

```shell
openssl rand -base64 32
```

不要将真实密钥写入配置文件或提交到仓库。响应加密密钥只有在标注了 `@EncryptResponse` 的接口被调用时才会校验；凭据密钥只有在加密或解密服务端凭据时才会校验。缺失、不是合法 Base64，或解码后不是 32 字节时，调用会失败。

方法级使用：

```java
@EncryptResponse
@GetMapping("/users/{id}")
public Result<User> get(@PathVariable Long id) {
    return Result.data(userService.findById(id));
}
```

类级使用会覆盖该 Controller 的全部接口：

```java
@EncryptResponse
@RestController
@RequestMapping("/users")
public class UserController {
    // ...
}
```

该注解仅支持 `Result<?>` 响应。`data == null` 时保持 `null`；`code`、`message`、`traceId` 和 `extra` 不变。非空 `data` 会先序列化为 JSON，再返回以下格式的字符串：

```text
v1.<Base64URL-IV>.<Base64URL-密文及GCM认证标签>
```

浏览器可以使用 Web Crypto API 解密：

```javascript
function decodeBase64(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  return Uint8Array.from(atob(padded), character => character.charCodeAt(0));
}

async function decryptResponseData(encryptedData, base64Key) {
  if (encryptedData == null) {
    return null;
  }

  const [version, encodedIv, encodedCiphertext] = encryptedData.split(".");
  if (version !== "v1" || !encodedIv || !encodedCiphertext) {
    throw new Error("Unsupported encrypted response format");
  }

  const key = await crypto.subtle.importKey(
    "raw",
    decodeBase64(base64Key),
    { name: "AES-GCM" },
    false,
    ["decrypt"]
  );
  const plaintext = await crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv: decodeBase64(encodedIv),
      tagLength: 128
    },
    key,
    decodeBase64(encodedCiphertext)
  );
  return JSON.parse(new TextDecoder().decode(plaintext));
}
```

> 响应加密不能替代 HTTPS。普通浏览器最终仍需持有解密能力，因此它不能阻止终端用户查看自己收到的数据。

### 服务端凭据加密

`PlatformCredentialCipher` 位于 `com.aurora.starter.webmvc.security`，用于保存邮件授权码、SSH 密码等服务端凭据：

```java
String ciphertext = cipher.encrypt("mail.auth-code", authorizationCode);
String authorizationCode = cipher.decrypt("mail.auth-code", ciphertext);
```

`purpose` 会作为 AES-GCM 的 AAD，防止同一密文被搬移到不相关的凭据字段。密文格式为：

```text
v1:<Base64(12 字节 IV)>:<Base64(密文及 128 位认证标签)>
```

响应报文密钥和服务端凭据密钥必须独立；不要把凭据密钥交给浏览器。

---

## 四、业务异常 `BizException`

抛出即被 `GlobalExceptionHandler` 捕获并转 `Result.error(...)`：

```java
if (user == null) {
    throw new BizException(DefaultBizCode.NOT_FOUND);
}
if (!password.matches(...)) {
    throw new BizException(DefaultBizCode.PARAM_INVALID, "密码格式不正确");
}
throw new BizException(10001, "余额不足");
```

### 自定义业务码

实现 `BizCode` 接口：

```java
@Getter
@AllArgsConstructor
public enum UserBizCode implements BizCode {
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_DISABLED(10002, "用户已禁用");

    private final int code;
    private final String message;
}

throw new BizException(UserBizCode.USER_NOT_FOUND);
```

---

## 五、全局异常处理器

`GlobalExceptionHandler` 自动处理以下异常并返回 `Result`：

| 异常 | 业务码 | 日志级别 |
| --- | --- | --- |
| `BizException` | `e.getCode()` | warn |
| `MethodArgumentNotValidException`（`@Valid` on `@RequestBody`） | 400 | warn |
| `BindException`（`@Valid` on `@ModelAttribute`/表单） | 400 | warn |
| `ConstraintViolationException`（`@Validated`） | 400 | warn |
| `HttpMessageNotReadableException`（请求体 JSON 解析失败） | 400 | warn |
| `MissingServletRequestParameterException` | 400 | warn |
| `NoHandlerFoundException`* | 404 | warn |
| 其它 `Throwable` | 500 | error（含全栈） |

> \* `NoHandlerFoundException` 默认 Spring 不抛，业务侧需开启：
> ```yaml
> spring:
>   mvc:
>     throw-exception-if-no-handler-found: true
>   web:
>     resources:
>       add-mappings: false
> ```

业务侧可在自己的 `@RestControllerAdvice` 中追加更高优先级 handler 覆盖默认行为。

---

## 六、TraceId 透传

`TraceIdFilter` 自动处理每个请求：

1. 从 header `X-Trace-Id` 取，缺失则生成 32 位 UUID（去横线）
2. 写入 SLF4J MDC（key=`traceId`）和 `TraceContext` ThreadLocal
3. 响应 header 回填 `X-Trace-Id`
4. 请求结束清理（防 ThreadLocal 泄漏）

### 日志输出 traceId

`platform-webmvc` 默认使用 **Log4j2**（`src/main/resources/log4j2-spring.xml`），pattern 中已包含 `%X{traceId:-}`，下游工程无需自己配置：

```xml
<Property name="PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level [%X{traceId:-}] %logger{36} - %msg%n</Property>
```

如果业务工程仍在用 Logback（旧工程未升级到 Log4j2），配置 pattern 中加 `%X{traceId}`：

```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

### 业务代码读取

```java
String traceId = TraceContext.get();
```

### 异步线程透传

`TraceIdFilter` 仅覆盖同步请求线程。异步任务需通过 `TaskDecorator` 自行透传：

```java
@Bean
public TaskDecorator traceDecorator() {
    return runnable -> {
        String traceId = TraceContext.get();
        return () -> {
            try {
                TraceContext.set(traceId);
                runnable.run();
            } finally {
                TraceContext.clear();
            }
        };
    };
}
```

---

## 七、请求日志拦截

`RequestLogFilter` 在每个请求 finally 时输出（INFO 级别）：

```
method=GET uri=/users/123 status=200 duration=12ms ip=127.0.0.1
```

IP 取值顺序：`X-Forwarded-For` 首段 → `X-Real-IP` → `request.getRemoteAddr()`。

### 默认排除路径

写死在 `RequestLogPathExcludes`，不打印日志：

- 路径模式：`/actuator/**`、`/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-resources/**`、`/doc.html`、`/webjars/**`、`/favicon.ico`
- 后缀：`.js .css .ico .png .jpg .jpeg .gif .svg .woff .woff2`

如需关闭整个 RequestLogFilter，业务侧用 `@ComponentScan` 的 `excludeFilters` 排除即可。

---

## 八、版本与依赖

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.0 |
| spring-boot-starter-web | 由 Spring Boot BOM 管控 |
| platform-common | 1.0.0-SNAPSHOT（传递引入 spring-boot-starter-validation） |
| knife4j-spring-boot-starter | 1.0.0-SNAPSHOT（传递引入 OpenAPI 3 `@Schema` 注解） |
