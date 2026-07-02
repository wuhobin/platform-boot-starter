# quartz-spring-boot-starter

基于 `spring-boot-starter-quartz` 的开箱即用任务调度 Starter。**业务方只需要写 Controller**——Entity、Mapper、Service、JobBootstrap、DbJobLogHandler 全部内置。

底层使用 **内存 Quartz**（不创建 QRTZ_* 等系统表），元数据存 `quartz_job` / `quartz_job_log` 表。

## 一、引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>quartz-spring-boot-starter</artifactId>
</dependency>
```

## 二、三步使用

### Step 1：建表

**方式 A：配置自动建表（推荐）**

```yaml
platform:
  quartz:
    auto-init-table: true   # 启动时自动 CREATE TABLE IF NOT EXISTS
```

首次启动后即可关掉（设为 `false` 或删除该行）。

**方式 B：手动执行 DDL**

```bash
mysql -u root -p your_db < sql/quartz_job.sql
```

### Step 2：写一个任务 Bean

```java
@Component("orderTask")
public class OrderTask {
    public void cleanExpiredOrders() {
        // 业务逻辑
    }

    public void doWithParams(String name, Long count) {
        // 业务逻辑
    }
}
```

方法名匹配 `quartz_job.invoke_target` 字符串即可（语法：`beanName.method('arg1',1L,2D,true)`）。

### Step 3：写 Controller

```java
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final IQuartzJobService quartzJobService;     // starter 内置

    @GetMapping
    public List<QuartzJob> list() {
        return quartzJobService.list();
    }

    @PostMapping
    public String create(@RequestBody QuartzJob job) {
        try { quartzJobService.createJob(job); return "ok"; }
        catch (TaskException e) { return "invokeTarget 已存在: " + job.getInvokeTarget(); }
        catch (Exception e) { return "创建失败: " + e.getMessage(); }
    }

    @PutMapping
    public String update(@RequestBody QuartzJob job) {
        try { quartzJobService.updateJob(job); return "ok"; }
        catch (TaskException e) { return "invokeTarget 已被占用: " + job.getInvokeTarget(); }
        catch (Exception e) { return "更新失败: " + e.getMessage(); }
    }

    @DeleteMapping("/{jobId}")
    public String delete(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        try { quartzJobService.deleteJob(jobId, group); return "ok"; }
        catch (Exception e) { return "删除失败: " + e.getMessage(); }
    }

    @PostMapping("/{jobId}/pause")
    public String pause(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        try { quartzJobService.pauseJob(jobId, group); return "ok"; }
        catch (Exception e) { return "暂停失败: " + e.getMessage(); }
    }

    @PostMapping("/{jobId}/resume")
    public String resume(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        try { quartzJobService.resumeJob(jobId, group); return "ok"; }
        catch (Exception e) { return "恢复失败: " + e.getMessage(); }
    }

    @PostMapping("/{jobId}/run")
    public String runNow(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        try { quartzJobService.triggerNow(jobId, group); return "ok"; }
        catch (Exception e) { return "触发失败: " + e.getMessage(); }
    }
}
```

**就这么多。** 业务方不用写 Entity、Mapper、Service、JobBootstrap、DbJobLogHandler，全是 starter 内置。

应用启动时 starter 会自动：

1. 扫描 `quartz_job` 表，把 `status='0'` 的任务全部塞进内存 Quartz
2. 每次任务执行完，自动写一条记录到 `quartz_job_log`

---

## 三、starter 内置组件清单

| 组件 | 包路径 | 说明 |
|------|------|------|
| `QuartzJob` | `com.aurora.starter.quartz.domain` | 任务元数据实体 |
| `QuartzJobLog` | `com.aurora.starter.quartz.domain` | 任务日志实体 |
| `QuartzJobMapper` / `QuartzJobLogMapper` | `com.aurora.starter.quartz.mapper` | MyBatis-Plus BaseMapper |
| `IQuartzJobService` / `QuartzJobServiceImpl` | `com.aurora.starter.quartz.service` | CRUD + 同步 Quartz + invokeTarget 唯一性校验 |
| `DbJobLogHandler` | `com.aurora.starter.quartz.core.handler.impl` | 默认 `JobLogHandler`，写 `quartz_job_log` |
| `JobBootstrap` | `com.aurora.starter.quartz.bootstrap` | `ApplicationRunner`，启动同步 `quartz_job` → Quartz |
| `ScheduleManager` / `DefaultScheduleManager` | `com.aurora.starter.quartz.core.schedule` | 高级服务 |
| `QuartzAutoConfiguration` | `com.aurora.starter.quartz.config` | 自动配置 + 建表初始化 |
| `JobContext` / `JobLogRecord` | `com.aurora.starter.quartz.core.job` | 任务运行时 POJO |

## 四、配置项

```yaml
platform:
  quartz:
    enabled: true                        # 总开关,默认 true
    auto-init-table: false               # 启动时自动建表,默认 false
    default-job-group: DEFAULT           # 创建任务时未指定 group 的默认值
    default-misfire-policy: DEFAULT      # DEFAULT / IGNORE_MISFIRES / FIRE_AND_PROCEED / DO_NOTHING
    log:
      max-exception-length: 2000         # 异常信息最大保留字符
      skip-list: []                      # 跳过日志的 invokeTarget 关键字列表
```

> Starter 使用 Spring Boot 默认的内存 Quartz，**不**创建 QRTZ_* 等系统表。

## 五、高级用法

### 5.1 自定义 `JobLogHandler`

业务方提供自己的 `JobLogHandler` Bean 即可覆盖默认的 `DbJobLogHandler`：

```java
@Component
public class CustomJobLogHandler implements JobLogHandler {
    @Override
    public void onSuccess(JobLogRecord r) { /* 自定义:发 MQ/写 ES */ }
    @Override
    public void onError(JobLogRecord r, Throwable e) { /* ... */ }
}
```

### 5.2 关闭启动同步

不需要 `JobBootstrap` 自动从 `quartz_job` 同步到 Quartz：

```yaml
platform:
  quartz:
    bootstrap:
      enabled: false
```

### 5.3 跳过某个任务的日志

`JobContext.skipLog=true` 跳过该任务的日志处理，等价于原 `aurora-scheduler` 中 `redisTimer` 黑名单。

### 5.4 `invokeTarget` 语法

`beanName.method('arg1',1L,2D,3,true)`，支持参数类型：

| 字面量 | 实际类型 | 示例 |
|------|------|------|
| `'xxx'` | `String` | `'hello'` |
| `数字L` | `Long` | `100L` |
| `数字D` | `Double` | `1.5D` |
| `true` / `false` | `Boolean` | `true` |
| 其他数字 | `Integer` | `42` |

## 六、注意事项

- **不要开启 Quartz JDBC 模式**：starter 默认内存存储，配合 `quartz_job` 实现元数据管理。不要加 `spring.quartz.job-store-type=jdbc` 之类配置，否则会引入 QRTZ_* 系统表，与 `quartz_job` 形成"两套真源"。
- **invokeTarget 唯一性**：`createJob` 和 `updateJob` 会自动校验 `invokeTarget` 不重复，重复时抛 `TaskException(TASK_EXISTS)`。
- `concurrent` 字段：`"0"` 允许并发，`"1"` 禁止并发。
- `misfirePolicy` 字符串值：`"0"` DEFAULT / `"1"` IGNORE_MISFIRES / `"2"` FIRE_AND_PROCEED / `"3"` DO_NOTHING。
- starter 默认使用 MyBatis-Plus 持久化。如果业务方使用其他 ORM，可以自己写 Entity/Mapper/Service，仅依赖 `ScheduleManager` / `JobContext` / `JobLogHandler` 这套核心抽象。
