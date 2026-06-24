# redis-platform-boot-starter

Redis 自动装配 Starter，基于 Spring Data Redis（Lettuce）+ Redisson，提供缓存操作、限流、延迟队列、重试任务、消息队列等能力。

## 功能特性

- ✅ `JsonRedisTemplate` — JSON 序列化的 RedisTemplate
- ✅ `RedisCache` — 缓存操作工具类，覆盖 String / List / Set / ZSet / Hash / 自增 / 扫描等场景
- ✅ `RedisBloomFilter` — 基于 Redisson RBloomFilter 的布隆过滤器，支持缓存穿透防护
- ✅ `RedisRateLimiter` — 基于 Redisson RRateLimiter 的限流处理器
- ✅ `DelayedTask` — 基于 Redisson RDelayedQueue 的延迟任务框架
- ✅ `DelayedRetryTask` — 带重试策略的延迟任务（可自定义重试次数、间隔）
- ✅ `RedisMessageQueue` — 基于 RBlockingQueue 的轻量消息队列
- ✅ `TwoLevelCache` — 基于 Caffeine L1 + Redis L2 的两级缓存，支持 Pub/Sub 多实例失效、防穿透、防击穿

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>redis-platform-boot-starter</artifactId>
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

## 一、缓存操作 — RedisCache

封装了常用 Redis 数据结构的便捷操作，注入即可使用：

```java
@Autowired
private RedisCache redisCache;
```

### String 操作

```java
// 存
redisCache.setCacheObject("user:1001", user);
// 带过期时间（10 分钟）
redisCache.setCacheObject("user:1001", user, 10, TimeUnit.MINUTES);
// 不存在才设置（分布式锁场景）
redisCache.setIfAbsent("lock:order:1001", "locked", 30, TimeUnit.SECONDS);
// 取
User user = redisCache.getCacheObject("user:1001");
// 删
redisCache.deleteObject("user:1001");
```

### List 操作

```java
redisCache.setCacheList("queue:tasks", taskList);
redisCache.addCacheList("queue:tasks", newTask);
List<Task> tasks = redisCache.getCacheList("queue:tasks");
```

### Set 操作

```java
redisCache.setCacheSet("tags:article:1", tagSet);
redisCache.addCacheSet("tags:article:1", "java", "spring");
Set<String> tags = redisCache.getCacheSet("tags:article:1");
```

### Hash 操作

```java
redisCache.setCacheMap("user:profile:1001", profileMap);
redisCache.setCacheMapValue("user:profile:1001", "nickname", "zhangsan");
String nickname = redisCache.getCacheMapValue("user:profile:1001", "nickname");
Map<String, Object> all = redisCache.getCacheMap("user:profile:1001");
```

### ZSet（有序集合）操作

```java
// 按分数插入，常用于排行榜
redisCache.addZset("rank:score", "player1", 980.0);
// 限制集合大小，保留最高分
redisCache.addZset("rank:score", "player2", 850.0, 100);
// 获取排行榜
Set<String> top = redisCache.getCacheZSet("rank:score");
// 分数从高到低
Set<String> reverse = redisCache.getCacheReverseZSet("rank:score");
```

### 自增/自减

```java
// 适用于计数器、限流计数
redisCache.increment("pv:article:1");           // +1
redisCache.increment("pv:article:1", 5);        // +5
redisCache.decrement("stock:1001");             // -1
// 首次自增同时设过期时间
redisCache.increment("counter:daily", 1, 1, TimeUnit.DAYS);
```

### 批量 & 扫描

```java
// 批量获取
List<User> users = redisCache.multiGet(Set.of("user:1", "user:2"));
// SCAN 扫描（大数据量推荐，不会阻塞 Redis）
Collection<String> keys = redisCache.scan("user:*");
// 按模式批量删除
redisCache.deleteByPattern("temp:*");
```

### 全局唯一 ID

```java
// 格式：ORDER + 当天日期 + 自增序号（4 位补零）
// 结果示例：ORDER202606180001
String orderNo = redisCache.soleId("ORDER", "", "order:seq", new Date());
// 指定 ID 长度
String serialNo = redisCache.soleId("SN", "", "serial:seq", new Date(), 6);
```

### 布隆过滤器防护

```java
// 带布隆过滤器的缓存查询，防止缓存穿透
// bloom filter 判定不存在 → 直接返回 null，不走 Redis
User user = redisCache.getCacheObject("user:1001", userBloomFilter);
```

## 二、布隆过滤器 — RedisBloomFilter

基于 Redisson `RBloomFilter`，提供声明式配置、缓存穿透防护和完整统计信息。

### 配置

```yaml
platform:
  redis:
    bloom-filter:
      enabled: true                # 总开关，默认 false
      filters:
        - name: user:bloom         # 必填，Redis key 名
          expected-insertions: 1000000   # 必填，预期插入量
          false-positive-probability: 0.01  # 必填，误判率
        - name: product:bloom
          expected-insertions: 5000000
          false-positive-probability: 0.03
          ttl: 30d                 # 可选，过期时间（不配则永久有效）
```

### 注入使用

```java
@Autowired
private Map<String, RedisBloomFilter<?>> bloomFilters;

// 按 name 获取
RedisBloomFilter<String> userBloom = (RedisBloomFilter<String>) bloomFilters.get("user:bloom");
```

### 基础操作

```java
// 添加元素
userBloom.add("user:1001");
userBloom.add("user:1002");

// 判断是否存在（false = 一定不存在，true = 可能存在）
boolean exists = userBloom.contains("user:1001");

// 近似元素数量
long count = userBloom.count();

// 统计信息（expectedInsertions / falsePositiveProbability / hashIterations / bitSize）
BloomFilterStats stats = userBloom.getStats();

// 删除整个布隆过滤器（不可逆）
userBloom.delete();
```

### 缓存穿透防护

```java
// 方式1：通过 RedisCache 便捷方法
User user = redisCache.getCacheObject("user:1001", userBloom);

// 方式2：通过 protect 方法，loader 可以是任意数据源
User user = userBloom.protect("user:1001", () -> {
    // 布隆过滤器说"可能存在"时才会执行
    return dbUserMapper.selectById(1001);
});
```

## 三、限流 — RedisRateLimiter

基于 Redisson `RRateLimiter`，支持阻塞等待和尝试获取两种模式：

```java
@Autowired
private RedisRateLimiter rateLimiter;

// 方式1：阻塞等待，拿不到许可就一直等
rateLimiter.rateLimit("api:sendSms", 10, 60, () -> {
    // 每分钟最多 10 次
    smsService.send(phone);
});

// 方式2：尝试获取，等 3 秒超时就丢弃
boolean acquired = rateLimiter.tryRateLimit("api:sendSms", 10, 60, 3, () -> {
    smsService.send(phone);
});

// 方式3：有返回值的限流
int result = rateLimiter.rateLimit("api:query", 100, 60, () -> {
    return heavyQuery();
});

// 方式5：有返回值的尝试限流，超时抛 RateLimiterException
try {
    User user = rateLimiter.tryRateLimit("api:getUser", 50, 60, 5, () -> {
        return userService.getById(id);
    });
} catch (RateLimiterException e) {
    // 限流处理
}

// 方式5：纯检查，不执行业务
if (!rateLimiter.tryAcquire("api:limit")) {
    throw new RateLimiterException("操作过于频繁");
}
```

## 四、延迟任务 — DelayedTask

基于 Redisson `RDelayedQueue`，消息在指定延迟后才被消费：

```java
@Component
public class OrderTimeoutTask extends DelayedTask<Order> {

    @Override
    public String getTaskGroup() {
        return "ORDER_TIMEOUT";
    }

    @Override
    public void consumer(Order order) {
        log.info("订单超时处理: {}", order.getId());
        // 检查订单是否已支付，未支付则取消
        orderService.cancelIfUnpaid(order.getId());
    }
}

// 使用：30 分钟后触发超时检查
orderTimeoutTask.producer(order, 30 * 60);
```

## 五、延迟重试任务 — DelayedRetryTask

继承 `DelayedTask`，增加了自动重试能力。失败后按配置的间隔自动重试：

```java
@Component
public class SmsRetryTask extends DelayedRetryTask<SmsParam> {

    @Override
    public String getTaskGroup() {
        return "SMS_RETRY";
    }

    @Override
    protected boolean execute(SmsParam param) {
        // 返回 false 或抛异常都会触发重试
        return smsClient.send(param.getPhone(), param.getContent());
    }
}

// 使用：发送失败后，默认最多重试 5 次，间隔递增（15s → 30s → 45s...）
DelayRetry<SmsParam> task = new DelayRetry<SmsParam>()
    .setData(smsParam)
    .setMaxCount(3)               // 最多重试 3 次
    .setInterval(10L)             // 每次间隔 10 秒
    .setUseSameInterval(true);    // 固定间隔（false=递增间隔）
smsRetryTask.producer(task, 0);  // 立即触发
```

## 六、消息队列 — RedisMessageQueue

基于 Redisson `RBlockingQueue` 的轻量阻塞队列：

```java
@Autowired
private RedisMessageQueue messageQueue;

// 生产者
Message<OrderEvent> msg = new Message<>();
msg.setMsgId(UUID.randomUUID().toString());
msg.setData(orderEvent);
messageQueue.send("ORDER_EVENT", msg);

// 消费者（阻塞等待，最多等 10 秒）
Message<OrderEvent> received = messageQueue.poll("ORDER_EVENT", 10);
if (received != null) {
    handleEvent(received.getData());
}
```

## 七、两级缓存 — TwoLevelCache

基于 Caffeine（L1 本地）+ Redis（L2 分布式）+ Redisson RTopic（失效通知），提供低延迟读写和跨实例缓存一致性。

### 配置

```yaml
platform:
  redis:
    two-level-cache:
      enabled: true                # 总开关，默认 false
      max-size: 10000              # Caffeine 全局默认最大条目数
      default-ttl: 300s            # L1 全局默认 TTL
      instances:                   # 可选，差异化实例
        - name: userCache
          max-size: 5000
          default-ttl: 600s
        - name: productCache
          max-size: 20000
          default-ttl: 120s
```

- 不配 `instances` → 仅默认实例
- `name: default` 可覆盖默认实例参数
- `max-size` / `default-ttl` 实例级不配时继承全局值

### 注入使用

```java
@Autowired
private TwoLevelCacheManager cacheManager;

// 默认实例
TwoLevelCache defaultCache = cacheManager.getDefault();

// 命名实例
TwoLevelCache userCache = cacheManager.get("userCache");
```

### 读取

```java
// 纯读（L1 → L2 穿透，不回源）
User user = cache.get("user:1001");

// 带回源 + 默认 TTL（L1 → L2 → loader，Caffeine 自动合并并发请求防击穿）
User user = cache.get("user:1001", () -> userMapper.selectById(1001));

// 带回源 + 自定义 TTL
User user = cache.get("user:1001", () -> userMapper.selectById(1001), 120, TimeUnit.SECONDS);
```

- `get(key)` 纯读：L1 miss → L2 miss → 返回 null
- `get(key, loader)` 带回源：L1 miss → L2 miss → 调 loader → 回填 L1 + L2
- `get(key, loader, ttl, unit)` 自定义 L1 TTL
- loader 返回 null 时自动缓存空值（TTL 60s）防穿透

### 写入

```java
// 写穿 L1 + L2 + Pub/Sub 广播通知其他实例失效
cache.set("user:1001", user, 300, TimeUnit.SECONDS);
```

### 删除

```java
// 删 L1 + L2 + 广播
cache.evict("user:1001");

// 批量删除
cache.evict(Set.of("user:1001", "user:1002"));

// 仅清本地 L1
cache.clearLocal();
```

更新 DB 后的标准流程：

```java
userMapper.updateById(user);   // 先更新 DB
cache.evict("user:1001");      // 再删缓存
```

### 架构

```
get(key, loader)
  ├── L1 (Caffeine) ──命中──→ return
  │    │ 未命中
  │    ↓
  ├── L2 (Redis) ──命中──→ 回填 L1 → return
  │    │ 未命中
  │    ↓
  └── loader.get() ──→ 回填 L2 → 回填 L1 → return

实例A evict("user:1")
  └── 删 L1 + 删 L2 + RTopic.publish
       ├── 实例B 收到 → L1.invalidate("user:1")
       └── 实例C 收到 → L1.invalidate("user:1")
```

## API 速览

| 类 | 用途 | 核心方法 |
|---|---|---|
| `RedisCache` | 缓存操作工具 | `setCacheObject` / `getCacheObject` / `setCacheList` / `setCacheSet` / `setCacheMap` / `increment` / `scan` / `soleId` |
| `RedisBloomFilter<T>` | 布隆过滤器 | `add` / `contains` / `count` / `delete` / `getStats` / `protect` |
| `RedisRateLimiter` | 限流 | `rateLimit` / `tryRateLimit` / `tryAcquire` |
| `DelayedTask<T>` | 延迟任务 | `producer(T, delay)` / `consumer(T)` / `getTaskGroup()` |
| `DelayedRetryTask<T>` | 延迟重试 | `execute(T)` / `hasNext(DelayRetry)` / `nextTime(DelayRetry)` |
| `RedisMessageQueue` | 消息队列 | `send(group, msg)` / `poll(group)` / `poll(group, timeout)` |
| `TwoLevelCache` | 两级缓存 | `get` / `get(key, loader)` / `set` / `evict` / `clearLocal` |
| `TwoLevelCacheManager` | 缓存实例管理 | `get(name)` / `getDefault()` / `names()` |
| `JsonRedisTemplate` | JSON 序列化 Template | 同 `RedisTemplate<String, Object>` |
| `DelayRetry<T>` | 重试策略模型 | `setMaxCount` / `setInterval` / `setUseSameInterval` |

## 注意事项

- Redisson 默认序列化已配置为 `JsonJacksonCodec`，存入 Redis 的数据为 JSON 格式，可直接在 `redis-cli` 中查看
- `RedisCache.keys()` 已标记 `@Deprecated`，大数据量场景请使用 `scan()` 或 `deleteByPattern()` 避免阻塞
- `RedisBloomFilter` 不配 `enabled: true` 则不创建任何 Bean，零侵入。布隆过滤器 `tryInit` 在应用重启时幂等（已存在则复用，不会抛异常）
- `RedisBloomFilter.getCacheObject(key, bloomFilter)` 会先查布隆过滤器再查 Redis，对存在的 key 会增加一次 Redis 往返（BF.EXISTS + GET）
- `RedisRateLimiter` 的限流配置 `rate` / `rateInterval` 在首次调用 `trySetRate` 时生效，后续调用不会更新
- `DelayRetry` 默认重试 5 次、间隔 15 秒，`useSameInterval=false` 时间隔按 `count × interval` 递增
- `RedisMessageQueue.poll()` 默认超时 5 秒，消费方需自行轮询调用
- `TwoLevelCache` 不配 `enabled: true` 则不创建任何 Bean，零侵入
- `TwoLevelCache.set()` 会通过 Pub/Sub 广播失效，本实例也会收到自己的消息导致 L1 被删（下次 get 从 L2 回填，仅多一次 L1 miss）
- `TwoLevelCache.get(key, loader)` 中 loader 返回 null 时缓存空值 60 秒防穿透，异常直接向上抛出不缓存
- 两级缓存的 L1 TTL 由每次 `set()`/`get()` 传入或使用实例默认值，Caffeine 的 `expireAfter` 按 key 动态决定过期时间
