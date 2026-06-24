# xlock-spring-boot-starter

Redisson 分布式锁自动装配 Starter，支持注解声明式加锁和编程式加锁两种方式。

## 功能特性

- ✅ `@XLock` 注解声明式加锁，零侵入
- ✅ 支持 6 种锁类型：可重入锁、公平锁、联锁、红锁、读锁、写锁
- ✅ SpEL 表达式动态解析锁 key
- ✅ `@XKey` 参数级注解绑定业务字段
- ✅ 支持 `waitTime`（等待时间）和 `leaseTime`（自动释放时间）
- ✅ 编程式 API：`LockService.lock(KeyInfo, Callable)` 适用于非 AOP 场景

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>xlock-spring-boot-starter</artifactId>
</dependency>
```

### 配置（application.yml）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Redisson 连接复用 `spring.data.redis` 配置，无需额外配置。

## 一、注解方式 — @XLock

### 基础用法

```java
@XLock(prefix = "ORDER", keys = {"#orderId"})
public void processOrder(String orderId) {
    // 同一 orderId 的请求串行执行
}
```

生成的锁 key：`xlock:key:ORDER-1001`

### 多 key 组合

```java
@XLock(prefix = "BILL_PUSH", keys = {"#form.billName", "#form.billType", "#form.buyerName"})
public void pushBill(BillForm form) {
    // 多维度锁定，防止相同单据重复推送
}
```

### 指定等待时间和自动释放

```java
// 最多等 5 秒获取锁，持有超过 30 秒自动释放
@XLock(prefix = "SEND_SMS", keys = {"#phone"}, waitTime = 5, leaseTime = 30)
public void sendSms(String phone) {
    // 发短信逻辑
}

// 不设置 leaseTime，Redisson watchdog 自动续期直到方法执行完毕
@XLock(prefix = "LONG_TASK", keys = {"#taskId"}, waitTime = 10)
public void longRunningTask(String taskId) {
    // 耗时较长的任务
}
```

### 指定锁类型

```java
// 公平锁：按请求顺序依次获取
@XLock(prefix = "QUEUE", keys = {"#id"}, lockType = XLockType.FAIR)
public void fairProcess(Long id) { }

// 读锁：多个读操作可并发
@XLock(prefix = "DATA", keys = {"#id"}, lockType = XLockType.READ)
public Data readData(Long id) { }

// 写锁：写操作独占，与读锁互斥
@XLock(prefix = "DATA", keys = {"#id"}, lockType = XLockType.WRITE)
public void writeData(Long id, Data data) { }

// 联锁：多把锁同时加锁，全部成功才算加锁成功
@XLock(prefix = "MULTI", keys = {"#a", "#b"}, lockType = XLockType.MULTI)
public void multiProcess(String a, String b) { }
```

### 自定义错误信息

```java
@XLock(prefix = "RUSH", keys = {"#skuId"}, waitTime = 2,
       errorMessage = "抢购人数过多，请稍后再试")
public void rushBuy(Long skuId) {
    // 获取锁失败时，抛出 LockException，消息为自定义内容
}
```

### 关闭日志

```java
@XLock(prefix = "HIGH_FREQ", keys = {"#id"}, disableLog = true)
public void highFrequencyMethod(Long id) {
    // 高频调用场景，关闭锁日志避免刷屏
}
```

## 二、参数注解 — @XKey

在方法参数上标注 `@XKey`，自动取参数值作为锁 key：

```java
@XLock(prefix = "USER", keys = {})
public void updateUser(@XKey Long userId, UserForm form) {
    // userId 自动作为锁 key
}
```

`@XKey` 也支持 SpEL：

```java
@XLock(prefix = "USER", keys = {})
public void updateUser(@XKey("getId") User user) {
    // 取 user.getId() 作为锁 key
}
```

## 三、编程方式 — LockService

适用于非 AOP 场景，比如在循环中分批加锁：

```java
@Autowired
private LockService lockService;

// 可重入锁（默认）
KeyInfo keyInfo = KeyInfo.builder()
    .prefix("ORDER")
    .keys(new String[]{String.valueOf(orderId)})
    .waitTime(5)
    .build();

String result = lockService.lock(keyInfo, () -> {
    // 业务逻辑
    return doSomething(orderId);
});

// 指定锁类型
KeyInfo readKeyInfo = KeyInfo.builder()
    .prefix("DATA")
    .keys(new String[]{String.valueOf(id)})
    .waitTime(3)
    .leaseTime(30)
    .build();

Data data = lockService.lock(readKeyInfo, XLockType.FAIR, () -> {
    return readFromDb(id);
});
```

## 四、@XLock 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `prefix` | String | 必填 | 锁 key 前缀，最终 key 格式为 `xlock:key:{prefix}-{key1}-{key2}` |
| `keys` | String[] | 必填 | SpEL 表达式数组，用于动态解析锁 key |
| `lockType` | XLockType | REENTRANT | 锁类型 |
| `waitTime` | long | -1 | 等待锁超时（默认单位秒），-1 表示不等待 |
| `leaseTime` | long | -1 | 锁自动释放时间（默认单位秒），-1 表示 watchdog 自动续期 |
| `timeUnit` | TimeUnit | SECONDS | 时间单位 |
| `errorMessage` | String | "" | 获取锁失败时的异常消息 |
| `disableLog` | boolean | false | 关闭锁日志 |

## 五、KeyInfo 参数说明

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `prefix` | String | 必填 | 锁 key 前缀 |
| `keys` | String[] | 必填 | 锁 key 值数组 |
| `waitTime` | long | -1 | 等待锁超时 |
| `leaseTime` | long | -1 | 锁自动释放时间 |
| `timeUnit` | TimeUnit | SECONDS | 时间单位 |
| `errorMessage` | String | null | 异常消息 |
| `disableLog` | boolean | false | 关闭日志 |

## 六、锁类型

| 类型 | 枚举值 | 说明 | 适用场景 |
|---|---|---|---|
| 可重入锁 | REENTRANT | 同一线程可多次获取 | 通用场景（默认） |
| 公平锁 | FAIR | 按请求顺序依次获取 | 需要保证公平性的场景 |
| 联锁 | MULTI | 多把锁同时加，全部成功才算加锁成功 | 需要同时锁定多个资源的场景 |
| 红锁 | RED | 多个独立 Redis 节点过半加锁成功 | 高可用场景 |
| 读锁 | READ | 多读并发，读写互斥 | 读多写少的数据 |
| 写锁 | WRITE | 写独占，与读锁和其他写锁互斥 | 数据写入 |

## 七、异常处理

```java
@ExceptionHandler(LockException.class)
public Result<?> handleLockException(LockException e) {
    return Result.error(429, e.getMessage());
}
```

## 注意事项

- `waitTime` 和 `leaseTime` 默认 `-1` 表示不启用，由 Redisson 默认行为接管
- 仅设置 `waitTime` 不设 `leaseTime`：等待指定时间获取锁，获取后 watchdog 自动续期直到方法结束
- 同时设置 `waitTime` 和 `leaseTime`：等待指定时间获取锁，持有超过 `leaseTime` 自动释放
- 仅设置 `leaseTime`：立即获取锁（不等待），持有超过 `leaseTime` 自动释放
- 都不设置：`lock.lock()` 一直等待直到获取锁，watchdog 自动续期
- 联锁/红锁的 `keys` 数组每个元素单独作为一个独立 Redis key，适合跨资源锁定
