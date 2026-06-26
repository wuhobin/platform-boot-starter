# Aurora Security Spring Boot Starter

基于 Sa-Token（Redis/Redisson 模式）的轻量级认证鉴权 Starter。

## 核心能力

- Sa-Token 自动装配（Redis 模式，复用 Redisson 连接）
- `SecurityUtils` 统一工具类 —— 封装登录/登出/鉴权操作
- `PermissionProvider` SPI —— 业务方实现权限/角色数据提供
- Sa-Token 异常 → 统一 `Result` 响应体
- `SaInterceptor` 路由拦截 —— 自动排除可配置的白名单路径

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
platform:
  security:
    token-name: Authorization   # Token 名称（前端 Header Key），默认 Authorization
    timeout: 604800             # Token 有效期（秒），默认 7 天
    exclude-paths:              # 业务方自定义放行路径（与 starter 默认开放路径自动合并去重）
      - /api/v1/auth/login      # 业务登录接口
      - /api/v1/auth/register   # 业务注册接口
```

**默认 Token 风格：** Bearer Token（前端请求 Header：`Authorization: Bearer <token>`），前后端分离模式，仅从 Header 读取 Token。

### 3. 实现权限提供者

```java
@Component
public class MyPermissionProvider implements PermissionProvider {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermsByUserId((Long) loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleService.getRolesByUserId((Long) loginId);
    }
}
```

### 4. 登录/鉴权

```java
@RestController
public class AuthController {
    @PostMapping("/api/v1/auth/login")
    public Result<?> login(@RequestBody LoginParam param) {
        User user = userService.authenticate(param.getUsername(), param.getPassword());
        SecurityUtils.login(user.getId());
        return Result.success(SecurityUtils.getTokenInfo());
    }
}

@RestController
public class UserController {
    @SaCheckPermission("sys:user:add")
    @PostMapping("/api/v1/users")
    public Result<?> createUser(@RequestBody UserDTO dto) { ... }
}
```

## 配置项

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `platform.security.enabled` | boolean | true | 是否启用 |
| `platform.security.token-name` | String | Authorization | Token 名称（前端 Header Key） |
| `platform.security.timeout` | int | 604800 | Token 有效期（秒），默认 7 天 |
| `platform.security.token-style` | String | uuid | Token 生成风格（uuid/simple-uuid/random-32/random-64/random-128/tik） |
| `platform.security.is-log` | boolean | false | 是否打印 Sa-Token 框架日志 |
| `platform.security.exclude-paths` | List\<String\> | swagger/actuator/error | 多账号下对所有 type 统一生效 |

## SecurityUtils API

| 方法 | 说明 |
|---|---|
| `login(Object userId)` | 登录 |
| `logout()` | 登出 |
| `isLogin()` | 是否已登录 |
| `getLoginId()` | 获取当前登录用户 ID |
| `getLoginIdAsLong()` | 获取当前登录用户 ID (Long) |
| `getLoginIdAsString()` | 获取当前登录用户 ID (String) |
| `hasPermission(String)` | 判断是否有权限 |
| `hasRole(String)` | 判断是否有角色 |
| `checkPermission(String)` | 校验权限 |
| `checkRole(String)` | 校验角色 |
| `kickout(Object userId)` | 踢人下线 |
| `getTokenValue()` | 获取当前 Token |
| `getTokenInfo()` | 获取 Token 信息对象 |

## 异常处理映射

| Sa-Token 异常 | 状态 | 消息 |
|---|---|---|
| `NotLoginException` | 401 UNAUTHORIZED | "未登录" |
| `NotPermissionException` | 403 FORBIDDEN | "无权限" |
| `NotRoleException` | 403 FORBIDDEN | "无权限" |
| `DisableServiceException` | 403 FORBIDDEN | "账号已被封禁" |
| `SaTokenException` | 500 SERVER_ERROR | 异常消息 |

## 多账号体系

适用于"同系统多端身份"场景（C 端 / 后台 / 商家等），
每种身份有独立的登录态、独立的 token 命名空间，互不污染。

> **类型安全：**使用 `AccountType` 枚举（`LOGIN` / `ADMIN` / `MERCHANT`），
> 只有 Sa-Token `@SaCheckLogin(type = "admin")` 等注解的 `type` 属性保留 String
> ——这是 Java 注解的限制，无法用枚举方法调用作为注解属性值。

### 1. 声明账号体系

```java
@Configuration
public class MyAccountConfig {
    @Bean
    public AccountTypeDefinition adminAccount() {
        return new SimpleAccountTypeDefinition(AccountType.ADMIN, List.of("/admin/**"), "后台管理员");
    }
    @Bean
    public AccountTypeDefinition merchantAccount() {
        return new SimpleAccountTypeDefinition(AccountType.MERCHANT, List.of("/merchant/**"), "商家");
    }
}
```

### 2. 登录 / 鉴权

```java
// Controller 登录
SecurityUtils.loginAs(AccountType.ADMIN, 10001L);     // 管理员登录
SecurityUtils.loginAs(AccountType.MERCHANT, 20001L);  // 商家登录

// 方法级注解鉴权（Sa-Token 原生注解，type 仍为 String——Java 注解限制）
@SaCheckLogin(type = "admin")
@SaCheckPermission(type = "admin", value = "user:add")
public Result<?> createUser(...) { ... }

// 编程式校验
SecurityUtils.checkLoginAs(AccountType.ADMIN);
SecurityUtils.checkPermissionAs(AccountType.MERCHANT, "order:ship");
```

### 3. 权限数据

`PermissionProvider` 接收 `AccountType loginType` 枚举参数，业务方在实现里 `switch` 分派：

```java
@Override
public List<String> getPermissionList(Object loginId, AccountType loginType) {
    return switch (loginType) {
        case ADMIN    -> adminPermService.listByUser((Long) loginId);
        case MERCHANT -> merchantPermService.listByUser((Long) loginId);
        default       -> List.of();
    };
}
```

### 路径匹配顺序

当 `paths` 中有重叠模式（如 `/admin/login` 与 `/admin/**`），
将**更具体的写在前面**，否则模糊匹配会先命中。

### 模式迁移

- **不声明任何 `AccountTypeDefinition` Bean**：旧 catch-all 行为，**无须任何改动**
- **声明了 `AccountTypeDefinition` Bean（且至少一个带 paths）**：进入显式多账号模式，
  仅校验各账号 paths 命中的 URL。若希望保留旧 catch-all 行为，可显式声明：

  ```java
  @Bean
  public AccountTypeDefinition loginAccount() {
      return new SimpleAccountTypeDefinition(AccountType.LOGIN, List.of("/**"), "default");
  }
  ```
