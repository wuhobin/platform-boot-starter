# platform-webmvc

> Spring Boot 3 Web MVC 通用件。沉淀统一响应体 `R<T>`、业务异常 `BizException`、全局异常处理器、TraceId 透传、请求日志拦截，下游 `<dependency>` 引入即用。**单模块通用件，不是 starter**——依赖 `@SpringBootApplication` 默认扫描 `com.aurora.starter.webmvc` 包；若下游包根不同，需用 `scanBasePackages` 追加。

![JDK](https://img.shields.io/badge/JDK-21+-blue.svg)
![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.5.0-blue.svg)

---

## 一、引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>platform-webmvc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

下游主类示例（包根非 `com.aurora.*` 时**必须**让 Spring 扫到 `com.aurora.starter.webmvc` 包，否则 Filter / `@RestControllerAdvice` 不生效）。两种写法二选一：

**推荐：`scanBasePackageClasses`（类型安全，IDE 重构友好）**

```java
import com.aurora.starter.webmvc.PlatformWebMvcMarker;

@SpringBootApplication(scanBasePackageClasses = {
    Application.class,            // 业务工程自身包
    PlatformWebMvcMarker.class    // platform-webmvc 包扫描锚点
})
public class Application { ... }
```

**或：`scanBasePackages` 字符串**

```java
@SpringBootApplication(scanBasePackages = {
    "com.example.user",           // 业务工程自身包
    "com.aurora.starter.webmvc"   // platform-webmvc 包
})
public class Application { ... }
```

> 业务工程自己的包**必须列出**，一旦写了 `scanBasePackages*`，默认扫描（主类所在包）就被覆盖。

---

## 二、统一响应体 `R<T>`

```java
@GetMapping("/users/{id}")
public R<User> get(@PathVariable Long id) {
    return R.ok(userService.findById(id));
}

@GetMapping
public R<Void> noContent() {
    return R.ok();
}
```

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 业务码，0=成功，其它=错误码 |
| `message` | String | 提示消息 |
| `data` | T | 业务数据，错误时为 null |
| `timestamp` | long | 服务器时间戳（毫秒） |
| `traceId` | String | 从 MDC 取，未配置 trace 时为 null |

> **HTTP 状态码**统一返回 200，业务结果由 `code` 字段承载。如需 4xx/5xx 走 HTTP 状态码，业务侧自行写 `@ExceptionHandler` 覆盖。

---

## 三、业务异常 `BizException`

抛出即被 `GlobalExceptionHandler` 捕获并转 `R.error(...)`：

```java
if (user == null) {
    throw new BizException(DefaultErrorCode.NOT_FOUND);
}
if (!password.matches(...)) {
    throw new BizException(DefaultErrorCode.PARAM_INVALID, "密码格式不正确");
}
throw new BizException(10001, "余额不足");
```

### 自定义错误码

实现 `ErrorCode` 接口：

```java
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_DISABLED(10002, "用户已禁用");

    private final int code;
    private final String message;
}

throw new BizException(UserErrorCode.USER_NOT_FOUND);
```

---

## 四、全局异常处理器

`GlobalExceptionHandler` 自动处理以下异常并返回 `R`：

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

## 五、TraceId 透传

`TraceIdFilter` 自动处理每个请求：

1. 从 header `X-Trace-Id` 取，缺失则生成 32 位 UUID（去横线）
2. 写入 SLF4J MDC（key=`traceId`）和 `TraceContext` ThreadLocal
3. 响应 header 回填 `X-Trace-Id`
4. 请求结束清理（防 ThreadLocal 泄漏）

### 日志输出 traceId

logback 配置 pattern 中加 `%X{traceId}`：

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

## 六、请求日志拦截

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

## 七、版本与依赖

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.0 |
| spring-boot-starter-web | 由 Spring Boot BOM 管控 |
| spring-boot-starter-validation | 由 Spring Boot BOM 管控 |
| platform-common | 1.0.0-SNAPSHOT |
