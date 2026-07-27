# Verification Spring Boot Starter

基于 Spring Mail 和 Redis 的验证码基础设施。当前版本只提供邮件验证码能力，不包含 Controller、注册、登录或密码重置等业务流程。

## 功能

- 使用 `SecureRandom` 生成可配置长度的纯数字验证码
- 同一“邮箱 + 业务场景”只保留一个有效验证码
- 同步发送文本或 HTML 邮件
- Redis 冷却控制，默认 60 秒
- Redis Lua 原子校验并消费，验证码只能成功使用一次
- 统一通过 `redis-spring-boot-starter` 提供的 `RedisCache` 操作 Redis
- 内置常用场景，并允许下游扩展场景枚举
- 默认关闭，显式启用后对必要配置执行启动校验

当前版本不提供短信验证码和图片验证码，不限制每小时/每天发送次数，也不限制验证码错误尝试次数。

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

## 发送验证码

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

## 校验并消费

```java
boolean verified = verificationService.verifyAndConsume(
        new MailVerificationVerifyRequest(
                email,
                CommonVerificationScene.REGISTER,
                code));
```

正确验证码会在 Redis 中原子删除并返回 `true`。验证码错误、过期或不存在统一返回 `false`；Redis 异常会抛出 `VerificationStorageException`。

## 自定义场景

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

## 异常

- `VerificationCooldownException`：仍处于发送冷却期，可通过 `getRetryAfter()` 获取剩余时间
- `VerificationDeliveryException`：邮件创建或 SMTP 投递失败
- `VerificationStorageException`：Redis 冷却、保存或校验失败
- `IllegalArgumentException`：邮箱、场景、主题、正文或验证码参数非法

## Redis 与日志安全说明

Redis Key 包含规范化后的明文邮箱，验证码值也以明文保存。示例 Key：

```text
app:verification:mail:code:register:user@example.com
app:verification:mail:cooldown:register:user@example.com
```

按照当前模块约定，发送和校验日志会记录完整邮箱及验证码，但不会记录邮件正文。这些日志包含在验证码有效期内可直接使用的认证凭据，生产环境必须严格限制日志访问权限、导出范围和保留周期。
