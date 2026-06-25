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
    exclude-paths:              # 放行路径
      - /api/v1/auth/login
      - /api/v1/auth/register
      - /swagger-resources/**
      - /v3/api-docs/**
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
| `platform.security.exclude-paths` | List\<String\> | `/api/v1/auth/login` 等 | 放行路径 |

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
