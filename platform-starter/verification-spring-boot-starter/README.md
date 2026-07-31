# Verification Spring Boot Starter

基于 Spring Mail、Redis、阿里云短信认证服务和 tianai-captcha 的验证码基础设施，提供邮件、短信和图片行为验证码能力。Starter 不包含 Controller、注册、登录或密码重置等业务流程，HTTP 接口由下游应用负责。

## 功能

### 邮件验证码

- 使用 `SecureRandom` 生成可配置长度的纯数字验证码
- 同一“邮箱 + 业务场景”只保留一个有效验证码
- 同步发送文本或 HTML 邮件
- Redis 冷却控制，默认 60 秒
- Redis Lua 原子校验并消费，验证码只能成功使用一次
- 统一通过 `redis-spring-boot-starter` 提供的 `RedisCache` 操作 Redis
- 内置常用场景，并允许下游扩展场景枚举
- 默认关闭，显式启用后对必要配置执行启动校验

### 图片验证码

- 集成 `tianai-captcha-springboot-starter` 1.5.5，直接使用其数据模型
- 服务端固定单一验证码类型，默认 `SLIDER`
- 四种标准类型共享内置的 5 张网络背景图，并提供带超时、进程内缓存的 URL 资源读取器
- 使用 tianai 内置二次验证，匹配成功后的 `captchaId` 是短期、一次性业务凭证
- 默认挑战有效期 120 秒，二次验证凭证有效期 60 秒
- 必须使用 Redis；启用时禁止静默降级到 tianai `LocalCacheStore`
- `ResourceStore` 和 `ImageVerificationService` 均允许下游 Bean 完全覆盖

### 短信验证码

- 本地固定生成 6 位纯数字验证码，通过阿里云短信认证服务同步发送并在本地校验
- 仅支持中国大陆手机号，接受 `138...`、`86138...` 和 `+86138...` 三种格式
- 固定签名 `恒创联众`、模板 `100001`，不允许配置或由单次请求覆盖
- 默认 60 秒冷却、每小时 5 次、北京时间自然日 10 次，配额按手机号跨场景累计
- Redis Lua 在发送前原子预占冷却、配额并保存验证码；明确发送失败时原子回滚
- 校验成功后原子消费；默认最多错误 5 次，第 5 次错误会删除验证码
- 自定义响应完整映射阿里云响应头、状态码和业务字段，但不暴露 `model.verifyCode`
- 默认关闭，仅支持 Starter 属性中的 AccessKey ID/Secret，不使用默认凭证链

邮件验证码不限制每小时/每天发送次数，也不限制验证码错误尝试次数。

## 引入依赖

使用 `platform-dependencies-bom` 后无需声明版本：

```xml
<dependency>
    <groupId>io.github.wuhobin</groupId>
    <artifactId>verification-spring-boot-starter</artifactId>
</dependency>
```

## 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  mail:
    host: smtp.example.com
    port: 465
    username: no-reply@example.com
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.ssl.enable: true

platform:
  verification:
    key-prefix: app:verification
    mail:
      enabled: true
      # 可选；未配置时使用 spring.mail.username
      from: no-reply@example.com
      from-name: Aurora 安全中心
      code-length: 6
      expire-time: 5m
      cooldown: 60s
    image:
      enabled: true
      # 可选：SLIDER、ROTATE、CONCAT、WORD_IMAGE_CLICK
      type: SLIDER
    sms:
      enabled: true
      access-key-id: ${ALIYUN_SMS_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_SMS_ACCESS_KEY_SECRET}
      # 以下均可省略并使用默认值
      expire-time: 5m
      cooldown: 60s
      hourly-limit: 5
      daily-limit: 10
      max-failed-attempts: 5
```

配置约束：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `platform.verification.key-prefix` | `verification` | Redis Key 前缀，生成 Key 时统一转换为小写 |
| `platform.verification.mail.enabled` | `false` | 是否启用邮件验证码 |
| `platform.verification.mail.from` | `spring.mail.username` | 可选的发件人邮箱覆盖项；两者均为空时启动失败 |
| `platform.verification.mail.from-name` | 无 | 可选的发件人显示名称 |
| `platform.verification.mail.code-length` | `6` | 验证码长度，范围 4～8 |
| `platform.verification.mail.expire-time` | `5m` | 有效期，范围 30 秒～30 分钟 |
| `platform.verification.mail.cooldown` | `60s` | 同一邮箱和场景的发送冷却时间，必须大于 0 |
| `platform.verification.image.enabled` | `false` | 是否启用平台图片验证码服务、资源和校验 |
| `platform.verification.image.type` | `SLIDER` | 固定生成类型，忽略大小写，仅支持四种标准类型 |
| `platform.verification.sms.enabled` | `false` | 是否启用阿里云短信验证码 |
| `platform.verification.sms.access-key-id` | 无 | 启用短信时必填，仅从 Starter 属性读取，配置对象的 `toString()` 不输出该值 |
| `platform.verification.sms.access-key-secret` | 无 | 启用短信时必填，仅从 Starter 属性读取，配置对象的 `toString()` 不输出该值 |
| `platform.verification.sms.expire-time` | `5m` | 有效期，范围 30 秒～30 分钟 |
| `platform.verification.sms.cooldown` | `60s` | 同一手机号和场景的发送冷却，必须大于 0 |
| `platform.verification.sms.hourly-limit` | `5` | 首次发送起滚动 60 分钟的手机号配额，必须大于 0 |
| `platform.verification.sms.daily-limit` | `10` | 北京时间自然日手机号配额，不小于小时配额 |
| `platform.verification.sms.max-failed-attempts` | `5` | 单个验证码最大错误次数，范围 1～10 |

图片验证码启用时，tianai 二次验证必须保持开启；若显式设置 `captcha.secondary.enabled=false`，应用会启动失败。Starter 在代码中提供 120 秒挑战有效期和 60 秒二次验证有效期等安全默认值，普通用户无需配置 tianai。高级用户仍可使用官方 `captcha.*` 配置覆盖包括过期时间在内的参数。

## 发送邮件验证码

正文由下游提供，必须包含 `{code}`。starter 还支持将 `{expireMinutes}` 替换为向上取整后的有效分钟数。验证码不会填充到邮件主题中。

```java
import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.scene.CommonVerificationScene;

public class RegisterService {

    private final MailVerificationService verificationService;

    public RegisterService(MailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    public void sendCode(String email) {
        verificationService.send(new MailVerificationSendRequest(
                email,
                CommonVerificationScene.REGISTER,
                "注册验证码",
                "<p>您的验证码是 <strong>{code}</strong>，{expireMinutes} 分钟内有效。</p>",
                MailContentType.HTML));
    }
}
```

一次调用只允许一个收件人，不支持逗号或分号分隔的地址，也不提供抄送或密送。

## 校验并消费邮件验证码

```java
boolean verified = verificationService.verifyAndConsume(
        new MailVerificationVerifyRequest(
                email,
                CommonVerificationScene.REGISTER,
                code));
```

正确验证码会在 Redis 中原子删除并返回 `true`。验证码错误、过期或不存在统一返回 `false`；Redis 异常会抛出 `VerificationStorageException`。

## 自定义邮件场景

starter 内置：

- `REGISTER`
- `LOGIN`
- `RESET_PASSWORD`
- `CHANGE_EMAIL`

下游可以扩展自己的枚举：

```java
public enum AccountVerificationScene implements VerificationScene {
    DELETE_ACCOUNT;

    @Override
    public String code() {
        return name();
    }
}
```

场景编码会转换为大写，并且必须匹配 `[A-Z0-9_-]{1,64}`。

## 短信验证码接入

Starter 只注册 `SmsVerificationService`，不提供 Controller。签名、模板和阿里云请求策略固定在代码中；发送请求只允许传手机号和场景：

```java
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.sms.SmsVerificationSendRequest;
import com.aurora.starter.verification.sms.SmsVerificationSendResponse;
import com.aurora.starter.verification.sms.SmsVerificationService;
import com.aurora.starter.verification.sms.SmsVerificationVerifyRequest;

public class SmsLoginService {

    private final SmsVerificationService verificationService;

    public SmsLoginService(SmsVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    public SmsVerificationSendResponse sendCode(String phoneNumber) {
        return verificationService.send(new SmsVerificationSendRequest(
                phoneNumber,
                CommonVerificationScene.LOGIN));
    }

    public boolean verify(String phoneNumber, String code) {
        return verificationService.verifyAndConsume(new SmsVerificationVerifyRequest(
                phoneNumber,
                CommonVerificationScene.LOGIN,
                code));
    }
}
```

`scene` 是验证码的隔离维度。同一手机号在 `LOGIN` 场景收到的验证码不能用于 `REGISTER`，但小时和每日发送配额仍按手机号跨场景累计。新验证码会覆盖同一“手机号 + 场景”的旧验证码；校验成功不会提前清除发送冷却。

阿里云请求固定使用 `SignName=恒创联众`、`TemplateCode=100001`、`ReturnVerifyCode=false`、`AutoRetry=0`，模板参数为 `{"code":"验证码","min":"有效分钟数"}`，`OutId` 自动生成为 UUID。连接超时为 3 秒、读取超时为 5 秒，SDK 自动重试关闭且最多只尝试一次。Starter 不调用阿里云校验接口。

只有阿里云返回 `Success=true` 且 `Code=OK` 才会正常返回 `SmsVerificationSendResponse`。返回对象包括 HTTP `headers`、`statusCode`，Body 的 `success`、`code`、`message`、`requestId`、`accessDeniedDetail`，以及 Model 的 `requestId`、`outId`、`bizId`；唯一排除阿里云响应中的 `model.verifyCode`。

发送前，Starter 使用单个 Redis Lua 脚本保存本次验证码并预占冷却、小时和每日配额。阿里云明确返回失败时，仅在验证码仍属于本次预占的前提下回滚；网络超时、连接中断等结果未知时保留验证码、冷却和配额，避免重复发送和重复计费。小时窗口从该窗口首次成功预占起滚动 60 分钟，每日窗口在 `Asia/Shanghai` 次日零点重置。

## 图片验证码接入

Starter 只提供 `com.aurora.starter.verification.image.ImageVerificationService`，不注册 Controller。下游可按自身的路由、统一响应和鉴权规范暴露生成与轨迹匹配接口：

```java
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.aurora.starter.verification.image.ImageVerificationService;

@RestController
@RequestMapping("/verification/image")
public class ImageVerificationController {

    private final ImageVerificationService verificationService;

    public ImageVerificationController(ImageVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    public ImageCaptchaVO generate() {
        return verificationService.generate();
    }

    @PostMapping("/{captchaId}/match")
    public boolean match(@PathVariable String captchaId,
                         @RequestBody ImageCaptchaTrack track) {
        return verificationService.match(captchaId, track);
    }
}
```

`generate()` 始终使用服务端配置的单一类型。`match()` 匹配成功后，原 `captchaId` 会被激活为 tianai 二次验证凭证；业务接口必须原子消费它：

```java
public void login(LoginRequest request) {
    if (!verificationService.verifyAndConsume(request.captchaId())) {
        throw new IllegalArgumentException("图片验证码无效或已过期");
    }
    // 继续登录业务流程
}
```

同一凭证只能消费一次，默认 60 秒过期，不绑定业务场景、账号、会话或 IP。轨迹错误、挑战过期、凭证不存在或重复消费返回 `false`；空参数抛出 `IllegalArgumentException`；资源、Redis 或生成器故障抛出 `ImageVerificationException`。

### 内置图片资源

默认 `ResourceStore` 使用以下 HTTPS 图片：

- `https://oss.wuhobin.top/base/20260418/20260418165311_949166a0.png`
- `https://oss.wuhobin.top/base/20260418/20260418165808_928ffbe5.png`
- `https://oss.wuhobin.top/base/20260418/20260418170001_567d69ad.png`
- `https://oss.wuhobin.top/base/20260418/20260418170105_613e5cae.png`
- `https://oss.wuhobin.top/base/20260418/20260418170445_3e29d1e0.png`

URL 读取的连接超时为 3 秒、读取超时为 5 秒，单个响应最大 10 MiB，最多缓存 32 个 URL；首次成功下载后按 URL 缓存在当前 JVM。首次生成依赖 OSS 可用性；下载失败会抛出图片资源异常。内置 `ResourceStore` 会将以上背景图同时注册给 `SLIDER`、`ROTATE`、`CONCAT` 和 `WORD_IMAGE_CLICK`。其中 `SLIDER`、`ROTATE` 所需模板以及 `WORD_IMAGE_CLICK` 所需字体由 tianai 的默认资源提供，因此使用内置 Store 时必须保持 `captcha.init-default-resource=true`（Starter 默认值）。自定义 `ResourceStore` Bean 存在时，内置 Store 自动退让。

下游也可以声明自己的 `ImageVerificationService` Bean 完全替换默认实现。两种覆盖均不需要排除自动配置。

### Redis、限流与日志

图片功能依赖可用的 Spring Data Redis 连接。启用后若 tianai 选择了 `LocalCacheStore`，启动校验会拒绝启动；显式提供自定义 `CacheStore` 时，下游必须保证其分布式和原子消费语义。

Starter 无法可靠识别客户端，因此不内置请求限流。生产环境必须在 Controller、网关或 WAF 对生成和轨迹匹配接口按 IP、会话及接口维度限流。

图片服务的 DEBUG 日志会记录完整 `captchaId` 和轨迹明细，但不会记录 Base64 图片；INFO 日志只记录操作结果。`captchaId` 在匹配成功后是可直接使用的短期凭证，生产环境应关闭相关 DEBUG 日志，并限制日志访问、导出和保留周期。

## 异常

- `VerificationCooldownException`：仍处于发送冷却期，可通过 `getRetryAfter()` 获取剩余时间
- `VerificationDeliveryException`：邮件创建或 SMTP 投递失败
- `SmsVerificationDeliveryException`：阿里云明确拒绝或发送结果未知；`getResponse()` 在明确失败时返回自定义响应，结果未知时为 `null`
- `VerificationRateLimitException`：短信小时或每日配额耗尽，可通过 `getType()` 和 `getRetryAfter()` 获取类型及剩余时间
- `VerificationStorageException`：Redis 冷却、保存或校验失败
- `ImageVerificationException`：图片资源、Redis 或验证码生成器发生系统故障
- `IllegalArgumentException`：邮箱、场景、主题、正文、`captchaId` 或轨迹参数非法

## 邮件验证码 Redis 与日志安全说明

Redis Key 包含规范化后的明文邮箱，验证码值也以明文保存。示例 Key：

```text
app:verification:mail:code:register:user@example.com
app:verification:mail:cooldown:register:user@example.com
```

按照当前模块约定，发送和校验日志会记录完整邮箱及验证码，但不会记录邮件正文。这些日志包含在验证码有效期内可直接使用的认证凭据，生产环境必须严格限制日志访问权限、导出范围和保留周期。

## 短信验证码 Redis 与日志安全说明

短信 Redis Key 包含规范化后的明文手机号，验证码值使用“预占令牌 + 明文验证码”保存。示例 Key：

```text
app:verification:sms:code:login:13800138000
app:verification:sms:attempts:login:13800138000
app:verification:sms:cooldown:login:13800138000
app:verification:sms:quota:hourly:13800138000
app:verification:sms:quota:daily:20260731:13800138000
```

按照当前模块约定，发送成功会在 INFO 日志记录完整手机号、场景、验证码、`requestId` 和 `bizId`；校验成功会在 INFO 日志记录完整手机号、场景和验证码；校验失败会在 WARN 日志记录完整手机号、场景、提交的验证码和累计错误次数。AccessKey ID/Secret 不会写入日志。

这些短信日志包含个人信息和在有效期内可直接使用的认证凭据。生产环境必须严格限制日志访问权限、导出范围和保留周期，并避免将其同步到权限边界不一致的第三方日志平台。
