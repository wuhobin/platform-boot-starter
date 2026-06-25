# redis-spring-boot-starter

Redis 自动装配 Starter，基于 Spring Data Redis（Lettuce）+ Redisson 双栈：Lettuce 提供 `RedisTemplate` 体系做日常 CRUD，Redisson 提供分布式能力（布隆过滤器、限流、Pub/Sub、阻塞队列、延迟任务、两级缓存失效广播等）。Redisson 默认使用 `JsonJacksonCodec`，存到 Redis 的值是 JSON，可用 `redis-cli` 直接查看。

## 功能特性

| 能力 | 对应类 | 说明 |
|------|--------|------|
| JSON 序列化 RedisTemplate | `JsonRedisTemplate` | String / List / Set / ZSet / Hash 通用 CRUD |
| 缓存操作工具 | `RedisCache` | 一站式便捷操作：CRUD、批量、SCAN、自增、唯一 ID、布隆防护查询 |
| 布隆过滤器 | `RedisBloomFilter<T>` | 声明式配置；穿透防护；统计信息 |
| 限流 | `RedisRateLimiter` | 阻塞 / 尝试 / 有返回值 / 纯检查多种 API |
| Pub/Sub 发布订阅 | `RedisPubSub` | 推送模式实时消息；`Subscription` 句柄可控取消 |
| 消息队列 | `RedisMessageQueue` | 基于 `RBlockingQueue` 的可靠队列；按 group 隔离 |
| 延迟任务 | `DelayedTask<T>` | 基于 `RDelayedQueue` 的延迟触发 |
| 延迟重试任务 | `DelayedRetryTask<T>` | 失败自动按策略重试 |
| 两级缓存 | `TwoLevelCache` + `TwoLevelCacheManager` | Caffeine L1 + Redis L2 + Pub/Sub 跨实例失效广播 |

## 模块结构

```
com.aurora.starter.redis
├── config          # 自动装配、Properties
├── core            # 业务核心组件（RedisCache / RedisPubSub / RedisMessageQueue / ...）
│   ├── manager     # 持有状态的 manager（TwoLevelCacheManager、AsyncManager）
│   └── task        # 延迟任务基类
├── model           # DTO（Message、DelayRetry、BloomFilterStats）
└── exception       # 业务异常（RateLimiterException）
```

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>redis-spring-boot-starter</artifactId>
</dependency>
```

### 基础配置（application.yml）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: xxx        # 可选
      # database: 0          # 可选
      # lettuce.pool:        # 可选，连接池配置
      #   max-active: 16
      #   max-idle: 8
```

引入 starter 后会自动装配：`RedisTemplate`、`RedisCache`、`RedisRateLimiter`、`RedisMessageQueue`、`RedisPubSub` 等常驻 Bean；`RedisBloomFilter`、`TwoLevelCache` 需通过 `platform.redis.*.enabled=true` 显式启用。

---

## 一、缓存操作 — `RedisCache`

封装了常用 Redis 数据结构的便捷操作，注入即可使用：

```java
@Autowired
private RedisCache redisCache;
```

### 1.1 String 操作

```java
// 存
redisCache.setCacheObject("user:1001", user);
// 带过期时间
redisCache.setCacheObject("user:1001", user, 10, TimeUnit.MINUTES);
// 不存在才设置（分布式锁场景）
redisCache.setIfAbsent("lock:order:1001", "locked", 30, TimeUnit.SECONDS);
// 取
User user = redisCache.getCacheObject("user:1001");
// 删
redisCache.deleteObject("user:1001");
```

### 1.2 List 操作

```java
redisCache.setCacheList("queue:tasks", taskList);
redisCache.addCacheList("queue:tasks", newTask);
List<Task> tasks = redisCache.getCacheList("queue:tasks");
```

### 1.3 Set 操作

```java
redisCache.setCacheSet("tags:article:1", tagSet);
redisCache.addCacheSet("tags:article:1", "java", "spring");
Set<String> tags = redisCache.getCacheSet("tags:article:1");
```

### 1.4 Hash 操作

```java
redisCache.setCacheMap("user:profile:1001", profileMap);
redisCache.setCacheMapValue("user:profile:1001", "nickname", "zhangsan");
String nickname = redisCache.getMapValue("user:profile:1001", "nickname");
Map<String, Object> all = redisCache.getCacheMap("user:profile:1001");
```

### 1.5 ZSet（有序集合）操作

```java
// 按分数插入，常用于排行榜
redisCache.addZset("rank:score", "player1", 980.0);
// 限制集合大小，只保留最高 100 个分数
redisCache.addZset("rank:score", "player2", 850.0, 100);
// 获取排行榜（分数从低到高）
Set<String> top = redisCache.getCacheZSet("rank:score");
// 分数从高到低
Set<String> reverse = redisCache.getCacheReverseZSet("rank:score");
```

### 1.6 自增 / 自减

```java
// 计数器、限流计数
redisCache.increment("pv:article:1");           // +1
redisCache.increment("pv:article:1", 5);        // +5
redisCache.decrement("stock:1001");             // -1
// 首次自增同时设置过期时间（用于"日活/日订单号"类计数）
redisCache.increment("counter:daily", 1, 1, TimeUnit.DAYS);
```

### 1.7 批量 & 扫描

```java
// 批量获取
List<User> users = redisCache.multiGet(Set.of("user:1", "user:2"));
// SCAN 扫描（大数据量推荐，不会阻塞 Redis）
Collection<String> keys = redisCache.scan("user:*");
// 按模式批量删除
redisCache.deleteByPattern("temp:*");
```

### 1.8 全局唯一 ID

```java
// 格式：ORDER + 当天日期(yyyyMMdd) + 自增序号（4 位补零）
// 结果示例：ORDER202606180001
String orderNo = redisCache.soleId("ORDER", "", "order:seq", new Date());
// 自定义序号长度（6 位）
String serialNo = redisCache.soleId("SN", "", "serial:seq", new Date(), 6);
```

### 1.9 布隆过滤器防护查询

```java
// bloom filter 判定不存在 → 直接返回 null，不走 Redis
User user = redisCache.getCacheObject("user:1001", userBloomFilter);
```

---

## 二、布隆过滤器 — `RedisBloomFilter`

基于 Redisson `RBloomFilter`，提供声明式配置、缓存穿透防护和完整统计信息。

### 2.1 配置

```yaml
platform:
  redis:
    bloom-filter:
      enabled: true                # 总开关，默认 false
      filters:
        - name: user:bloom         # 必填，Redis key 名
          expected-insertions: 1000000       # 必填，预期插入量
          false-positive-probability: 0.01   # 必填，误判率
        - name: product:bloom
          expected-insertions: 5000000
          false-positive-probability: 0.03
          ttl: 30d                 # 可选，过期时间（不配则永久有效）
```

### 2.2 注入使用

```java
@Autowired
private Map<String, RedisBloomFilter<?>> bloomFilters;  // Bean 名 redisBloomFilters

// 按 name 获取（注意强转目标元素类型）
RedisBloomFilter<String> userBloom = (RedisBloomFilter<String>) bloomFilters.get("user:bloom");
```

### 2.3 基础操作

```java
// 添加元素
userBloom.add("user:1001");
userBloom.add("user:1002");

// 判断是否存在
//   false = 一定不存在；true = 可能存在（受误判率约束）
boolean exists = userBloom.contains("user:1001");

// 近似元素数量
long count = userBloom.count();

// 统计信息：expectedInsertions / falsePositiveProbability / hashIterations / bitSize
BloomFilterStats stats = userBloom.getStats();

// 删除整个布隆过滤器（不可逆，连同 Redis 中的位图一起删除）
userBloom.delete();
```

### 2.4 缓存穿透防护

```java
// 方式 1：通过 RedisCache 便捷方法
User user = redisCache.getCacheObject("user:1001", userBloom);

// 方式 2：通过 protect 方法，loader 可以是任意数据源
User user = userBloom.protect("user:1001", () -> {
    // 布隆过滤器说"可能存在"时才会执行
    return dbUserMapper.selectById(1001);
});
```

---

## 三、限流 — `RedisRateLimiter`

基于 Redisson `RRateLimiter`，覆盖阻塞、尝试、有返回值、纯检查等所有典型场景。

```java
@Autowired
private RedisRateLimiter rateLimiter;
```

### 3.1 阻塞限流（拿不到许可一直等）

```java
// 无返回值：每分钟最多 10 次
rateLimiter.rateLimit("api:sendSms", 10, 60, () -> {
    smsService.send(phone);
});

// 有返回值：每分钟最多 100 次
User user = rateLimiter.rateLimit("api:query", 100, 60, () -> {
    return heavyQuery();
});
```

### 3.2 尝试限流（超时丢弃）

```java
// 无返回值：超时返回 false
boolean acquired = rateLimiter.tryRateLimit("api:sendSms", 10, 60, 3, () -> {
    smsService.send(phone);
});

// 有返回值：超时抛 RateLimiterException
try {
    User user = rateLimiter.tryRateLimit("api:getUser", 50, 60, 5, () -> {
        return userService.getById(id);
    });
} catch (RateLimiterException e) {
    // 限流处理
}
```

### 3.3 纯检查（不执行业务）

```java
if (!rateLimiter.tryAcquire("api:limit")) {
    throw new RateLimiterException("操作过于频繁");
}
```

### 3.4 自定义时间单位

```java
// 5 秒内最多 1 次
rateLimiter.rateLimit("api:heavy", 1, 5, RateIntervalUnit.SECONDS, () -> {
    heavyWork();
});
```

---

## 四、Pub/Sub 发布订阅 — `RedisPubSub`

基于 Redisson `RTopic`，提供推送模式的实时消息收发。**所有 topic 名会自动加 `pubsub:topic:` 前缀**（通过 `RedisKeyUtil.generate` 拼接），避免与业务自身 key 冲突。

### 4.1 注入使用

```java
@Autowired
private RedisPubSub redisPubSub;
```

### 4.2 发布消息

```java
// 立即发送到 topic，返回收到消息的订阅者数量
long receivers = redisPubSub.publish("order:event", orderEvent);
```

### 4.3 订阅消息（订阅句柄可控取消）

```java
// 订阅 —— 返回 Subscription 句柄
RedisPubSub.Subscription sub = redisPubSub.subscribe(
    "order:event",
    OrderEvent.class,
    event -> {
        // 收到消息时的处理逻辑
        log.info("收到订单事件: {}", event);
        handleEvent(event);
    }
);

// 不再需要时取消订阅
sub.unsubscribe();
```

订阅失败时 listener 抛出的异常会被本组件捕获并打 `ERROR` 日志，**不会导致整个订阅链路断开**。

### 4.4 批量取消某 topic 的所有订阅

```java
// 取消 "order:event" 这个 topic 上的所有订阅
redisPubSub.unsubscribeTopic("order:event");
```

> 适用场景：业务下线某个 topic 时一次性清理，**无需保留每个 `Subscription` 句柄**。

### 4.5 生命周期

应用关闭时，组件会自动取消所有 topic 的所有订阅（`@PreDestroy`）。

### 4.6 Pub/Sub vs MessageQueue 选型

| 维度 | Pub/Sub | MessageQueue |
|------|---------|--------------|
| 投递语义 | 至多一次（at-most-once） | 至少一次（持久化 + 阻塞拉取） |
| 消费模式 | 广播（所有订阅者都收到） | 竞争消费（一条只被一个消费者拿到） |
| 时间耦合 | 强：发布时订阅者必须在线 | 解耦：消费者离线时消息保留 |
| 适用 | 实时通知、状态广播、缓存失效 | 任务分发、削峰填谷、延迟任务 |

---

## 五、消息队列 — `RedisMessageQueue`

基于 Redisson `RBlockingQueue`，按 group 隔离的可靠消息队列。生产端异步投放（失败会写日志），消费端阻塞拉取。

```java
@Autowired
private RedisMessageQueue messageQueue;
```

### 5.1 发送消息

```java
Message<OrderEvent> msg = new Message<>();
msg.setMsgId(UUID.randomUUID().toString());
msg.setData(orderEvent);
messageQueue.send("ORDER_EVENT", msg);
```

> `send` 是 fire-and-forget；底层异步失败会写 ERROR 日志，**不会**导致消息丢失感知不到。

### 5.2 阻塞拉取

```java
// 默认阻塞 5 秒
Message<OrderEvent> received = messageQueue.poll("ORDER_EVENT");
if (received != null) {
    handleEvent(received.getData());
}

// 自定义超时（10 秒）
Message<OrderEvent> received = messageQueue.poll("ORDER_EVENT", 10, TimeUnit.SECONDS);
```

> **重要**：`poll` 会阻塞当前线程直到超时或拿到消息，**必须**在专用工作线程调用，禁止在 Tomcat/NIO 等请求线程直接调用，否则会拖垮请求处理。

### 5.3 key 命名

队列在 Redis 中的实际 key 为 `MESSAGE_QUEUE:{group}`（通过 `RedisKeyUtil.generate` 拼接）。

---

## 六、延迟任务 — `DelayedTask<T>`

基于 Redisson `RDelayedQueue`，消息在指定延迟后才被消费。继承基类并实现 `getTaskGroup()` / `consumer(...)` 即可：

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
orderTimeoutTask.producer(order, 30 * 60);  // delay 单位：秒
```

---

## 七、延迟重试任务 — `DelayedRetryTask<T>`

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

// 使用：发送失败后最多重试 3 次，固定间隔 10 秒
DelayRetry<SmsParam> task = new DelayRetry<SmsParam>()
    .setData(smsParam)
    .setMaxCount(3)               // 最多重试 3 次
    .setInterval(10L)             // 每次间隔 10 秒
    .setUseSameInterval(true);    // 固定间隔（false=递增间隔）
smsRetryTask.producer(task, 0);  // 立即触发
```

> `DelayRetry` 默认重试 5 次、间隔 15 秒，`useSameInterval=false` 时间隔按 `count × interval` 递增。

---

## 八、两级缓存 — `TwoLevelCache` + `TwoLevelCacheManager`

基于 Caffeine（L1 本地）+ Redis（L2 分布式）+ Redisson `RTopic`（失效广播），提供低延迟读写和跨实例缓存一致性。

### 8.1 配置

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

### 8.2 注入使用

```java
@Autowired
private TwoLevelCacheManager cacheManager;

// 默认实例
TwoLevelCache defaultCache = cacheManager.getDefault();

// 命名实例
TwoLevelCache userCache = cacheManager.get("userCache");

// 全部实例名
Set<String> names = cacheManager.names();
```

### 8.3 读取

```java
// 纯读（L1 → L2 穿透，不回源）
User user = cache.get("user:1001");

// 带回源 + 默认 TTL（L1 → L2 → loader）
// Caffeine 自动合并并发请求防击穿
User user = cache.get("user:1001", () -> userMapper.selectById(1001));

// 带回源 + 自定义 TTL
User user = cache.get("user:1001", () -> userMapper.selectById(1001), 120, TimeUnit.SECONDS);
```

行为细节：

- `get(key)` 纯读：L1 miss → L2 miss → 返回 `null`
- `get(key, loader)` 带回源：L1 miss → L2 miss → 调 loader → 回填 L1 + L2
- `get(key, loader, ttl, unit)` 自定义 L1 TTL
- loader 返回 `null` 时自动缓存空值（TTL 60s）防穿透
- loader 抛异常时**不缓存**，异常向上抛出

### 8.4 写入

```java
// 写穿 L1 + L2 + Pub/Sub 广播通知其他实例失效
cache.set("user:1001", user, 300, TimeUnit.SECONDS);
```

### 8.5 删除

```java
// 删 L1 + L2 + 广播失效
cache.evict("user:1001");

// 批量删除
cache.evict(Set.of("user:1001", "user:1002"));

// 仅清本地 L1（不碰 Redis，不广播）
cache.clearLocal();
```

更新 DB 后的标准流程：

```java
userMapper.updateById(user);   // 先更新 DB
cache.evict("user:1001");      // 再删缓存（Cache Aside 模式）
```

### 8.6 架构

```
get(key, loader)
  ├── L1 (Caffeine) ──命中──→ return
  │    │ 未命中
  │    ↓
  ├── L2 (Redis) ──命中──→ 回填 L1 → return
  │    │ 未命中
  │    ↓
  └── loader.get() ──→ 回填 L2 → 回填 L1 → return

实例 A evict("user:1")
  └── 删 L1 + 删 L2 + RTopic.publish
       ├── 实例 B 收到 → L1.invalidate("user:1")
       └── 实例 C 收到 → L1.invalidate("user:1")
```

> 本实例也会收到自己的失效消息（pub/sub 语义），导致 L1 被删 —— 下次 get 会从 L2 回填，**仅多一次 L1 miss**。

---

## API 速览

| 类 | 用途 | 核心方法 |
|---|---|---|
| `JsonRedisTemplate` | JSON 序列化 Template | 同 `RedisTemplate<String, Object>` |
| `RedisCache` | 缓存操作工具 | `setCacheObject` / `getCacheObject` / `setCacheList` / `setCacheSet` / `setCacheMap` / `addZset` / `increment` / `scan` / `deleteByPattern` / `soleId` |
| `RedisBloomFilter<T>` | 布隆过滤器 | `add` / `contains` / `count` / `delete` / `getStats` / `protect` |
| `RedisRateLimiter` | 限流 | `rateLimit` / `tryRateLimit` / `tryAcquire` |
| `RedisPubSub` | Pub/Sub 发布订阅 | `publish` / `subscribe` / `unsubscribeTopic` / `Subscription.unsubscribe` |
| `RedisMessageQueue` | 阻塞消息队列 | `send(group, msg)` / `poll(group)` / `poll(group, timeout, unit)` |
| `DelayedTask<T>` | 延迟任务 | `producer(T, delay)` / `consumer(T)` / `getTaskGroup()` |
| `DelayedRetryTask<T>` | 延迟重试 | `execute(T)` / `hasNext(DelayRetry)` / `nextTime(DelayRetry)` |
| `TwoLevelCache` | 两级缓存 | `get` / `get(key, loader)` / `set` / `evict` / `clearLocal` |
| `TwoLevelCacheManager` | 缓存实例管理 | `get(name)` / `getDefault()` / `names()` |
| `Message<T>` | 队列消息模型 | `msgId` / `data` |
| `DelayRetry<T>` | 重试策略模型 | `setMaxCount` / `setInterval` / `setUseSameInterval` |

## key 命名约定

所有写入 Redis 的 key 都通过 `RedisKeyUtil.generate(PREFIX, name)` 拼接，便于 `redis-cli KEYS '前缀:*'` 检索和管理：

| 组件 | 前缀常量 | 实际 key 格式 |
|------|---------|----------------|
| `RedisPubSub` | `pubsub:topic` | `pubsub:topic:{topic}` |
| `RedisMessageQueue` | `MESSAGE_QUEUE` | `MESSAGE_QUEUE:{group}` |
| `RedisRateLimiter` | `RATE_LIMITER` | `RATE_LIMITER:{limitKey}` |
| `RedisBloomFilter`（自动配置） | `BLOOM_FILTER` | `BLOOM_FILTER:{name}` |
| `TwoLevelCache` 失效广播 | `two-level-cache:evict:` | `two-level-cache:evict:{name}` |

## 注意事项

- **序列化**：Redisson 默认使用 `JsonJacksonCodec`，所有存入 Redis 的对象都是 JSON 格式，可用 `redis-cli` 直接查看；JSON 不存储 `@class` 类型信息，反序列化时调用方需保证类型一致。
- **`RedisCache.keys()` 已废弃**：大数据量场景请使用 `scan()` 或 `deleteByPattern()` 避免阻塞 Redis。
- **`RedisBloomFilter`**：不配 `enabled: true` 则不创建任何 Bean，零侵入；`tryInit` 在应用重启时幂等（已存在则复用，不会抛异常）。
- **`RedisBloomFilter` 缓存查询**：`redisCache.getCacheObject(key, bloomFilter)` 会先查 BF 再查 Redis，对存在的 key 会增加一次往返（`BF.EXISTS` + `GET`），**仅在防护穿透场景下值得使用**。
- **`RedisRateLimiter`**：`rate` / `rateInterval` 在首次调用 `trySetRate` 时生效，后续调用不会更新；每个 limitKey 对应的限流器在 Redis 中保留 1 天后过期。
- **`RedisPubSub`**：
  - topic 名前缀 `pubsub:topic:` 由组件自动添加，**调用方无需关心**。
  - listener 抛异常会被捕获并打 ERROR 日志，**不会**让整个订阅链路断开。
  - 应用关闭时通过 `@PreDestroy` 自动清理所有订阅。
- **`RedisMessageQueue`**：
  - `poll` 默认超时 5 秒，**必须**在专用工作线程调用。
  - `send` 是 fire-and-forget，异步失败会写 ERROR 日志。
- **`TwoLevelCache`**：
  - 不配 `enabled: true` 则不创建任何 Bean，零侵入。
  - `set()` 会通过 Pub/Sub 广播失效，本实例也会收到自己的消息（pub/sub 语义天然如此），导致 L1 被删 —— 下次 get 从 L2 回填，**仅多一次 L1 miss**。
  - `get(key, loader)` 中 loader 返回 `null` 时缓存空值 60 秒防穿透；loader 抛异常**不缓存**，异常直接向上抛出。
  - L1 TTL 由每次 `set()` / `get(key, loader, ttl, unit)` 传入或使用实例默认值，Caffeine 的 `expireAfter` 按 key 动态决定过期时间。
- **Pub/Sub vs MessageQueue 选型**：需要可靠投递、消费者离线容忍 → 用 `RedisMessageQueue`；需要实时广播、缓存失效通知 → 用 `RedisPubSub`。两者底层 Redis 资源（key）隔离，可同时使用。