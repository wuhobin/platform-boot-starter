# quartz-spring-boot-starter

基于 `spring-boot-starter-quartz` 的开箱即用任务调度 Starter。**业务方只需要写 Controller**——Entity、Mapper、Service、JobBootstrap、DbJobLogHandler 全部内置。

底层使用 **内存 Quartz**（不创建 QRTZ_* 等系统表），元数据存业务方自己的 `quartz_job` / `quartz_job_log` 表。

## 一、引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>quartz-spring-boot-starter</artifactId>
</dependency>
```

## 二、三步使用

### Step 1：建表

执行 starter 自带的 DDL：

```bash
# 路径: starter jar 内的 sql/quartz_job.sql
mysql -u root -p your_db < sql/quartz_job.sql
```

或在 `application.yml` 配 `spring.sql.init` 让 Spring Boot 启动时自动跑：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:sql/quartz_job.sql
```

DDL 内容（也可在 starter 源码 `src/main/resources/sql/quartz_job.sql` 查看）：

```sql
CREATE TABLE `quartz_job` (
    `job_id`          BIGINT       NOT NULL COMMENT '任务ID',
    `job_name`        VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(64)           DEFAULT 'DEFAULT' COMMENT '任务分组',
    `cron_expression` VARCHAR(255) NOT NULL COMMENT 'Cron表达式',
    `invoke_target`   VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    `concurrent`      CHAR(1)               DEFAULT '0' COMMENT '是否并发 0=允许 1=禁止',
    `misfire_policy`  CHAR(1)               DEFAULT '0' COMMENT 'misfire策略 0/1/2/3',
    `status`          CHAR(1)               DEFAULT '0' COMMENT '状态 0=正常 1=暂停',
    PRIMARY KEY (`job_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务表';

CREATE TABLE `quartz_job_log` (
    `log_id`         BIGINT       NOT NULL COMMENT '日志ID',
    `job_id`         BIGINT                DEFAULT NULL COMMENT '任务ID',
    `job_name`       VARCHAR(64)           DEFAULT NULL,
    `job_group`      VARCHAR(64)           DEFAULT NULL,
    `invoke_target`  VARCHAR(500)          DEFAULT NULL,
    `start_time`     DATETIME              DEFAULT NULL,
    `stop_time`      DATETIME              DEFAULT NULL,
    `cost_millis`    BIGINT                DEFAULT NULL,
    `job_message`    VARCHAR(500)          DEFAULT NULL,
    `status`         CHAR(1)               DEFAULT '0' COMMENT '0=成功 1=失败',
    `exception_info` TEXT                  COMMENT '异常信息',
    PRIMARY KEY (`log_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务执行日志表';
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

    @GetMapping("/list")
    public List<QuartzJob> list() {
        return quartzJobService.list();
    }

    @PostMapping
    public boolean create(@RequestBody QuartzJob job) {
        return quartzJobService.createJob(job);
    }

    @PutMapping
    public boolean update(@RequestBody QuartzJob job) {
        return quartzJobService.updateJob(job);
    }

    @DeleteMapping("/{jobId}")
    public boolean delete(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        return quartzJobService.deleteJob(jobId, group);
    }

    @PostMapping("/{jobId}/pause")
    public boolean pause(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        return quartzJobService.pauseJob(jobId, group);
    }

    @PostMapping("/{jobId}/resume")
    public boolean resume(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        return quartzJobService.resumeJob(jobId, group);
    }

    @PostMapping("/{jobId}/run")
    public boolean runNow(@PathVariable Long jobId, @RequestParam(defaultValue = "DEFAULT") String group) {
        return quartzJobService.triggerNow(jobId, group);
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
| `IQuartzJobService` / `QuartzJobServiceImpl` | `com.aurora.starter.quartz.service` | CRUD + 同步 Quartz |
| `DbJobLogHandler` | `com.aurora.starter.quartz.handler.impl` | 默认 `JobLogHandler`，写 `quartz_job_log` |
| `JobBootstrap` | `com.aurora.starter.quartz.bootstrap` | `ApplicationRunner`，启动同步 `quartz_job` → Quartz |
| `ScheduleManager` / `DefaultScheduleManager` | `com.aurora.starter.quartz.core.schedule` | 高级服务（create/pause/resume/delete/trigger/checkExists/isValidCron） |
| `QuartzProperties` | `com.aurora.starter.quartz.config` | `platform.quartz.*` 配置 |
| `sql/quartz_job.sql` | classpath:sql/ | 建表 DDL |
| `JobContext` / `JobLogRecord` | `com.aurora.starter.quartz.core.job` | 任务运行时 POJO（业务方一般不用直接接触） |

## 四、配置项

```yaml
platform:
  quartz:
    enabled: true                        # 总开关,默认 true
    default-job-group: DEFAULT           # 创建任务时未指定 group 的默认值
    default-misfire-policy: DEFAULT      # DEFAULT / IGNORE_MISFIRES / FIRE_AND_PROCEED / DO_NOTHING
    log:
      max-exception-length: 2000         # 异常信息最大保留字符
      skip-list: []                      # 跳过日志的 invokeTarget 关键字列表
```

> Starter 使用 Spring Boot 默认的内存 Quartz，**不**创建 QRTZ_* 等系统表。任务的元数据存业务方自己的 `quartz_job` 表。

## 五、高级用法

### 5.1 自定义 `JobLogHandler`

业务方提供自己的 `JobLogHandler` Bean 即可覆盖默认的 `DbJobLogHandler`：

```java
@Component
@Primary
public class CustomJobLogHandler implements JobLogHandler {
    @Override
    public void onSuccess(JobLogRecord r) { /* 自定义:发 MQ/写 ES */ }
    @Override
    public void onError(JobLogRecord r, Throwable e) { /* ... */ }
}
```

### 5.2 关闭启动同步

不需要 `JobBootstrap` 自动从 `quartz_job` 同步到 Quartz（比如纯单测环境）：

```yaml
platform:
  quartz:
    bootstrap:
      enabled: false
```

### 5.3 跳过某个任务的日志

在 `quartz_job` 任务元数据里加一个 `skip_log` 字段（业务方扩展 `QuartzJob` 实体后即可使用），传给 `JobContext.skipLog=true` 即可跳过该任务的日志处理。

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
- `concurrent` 字段：`"0"` 允许并发（用 `QuartzJobExecution`），`"1"` 禁止并发（用 `QuartzDisallowConcurrentExecution` + `@PersistJobDataAfterExecution`）。
- `misfirePolicy` 字符串值与 `MisfirePolicy` 枚举值一致：`"0"` DEFAULT / `"1"` IGNORE_MISFIRES / `"2"` FIRE_AND_PROCEED / `"3"` DO_NOTHING。
- `JobContext.skipLog=true` 跳过该任务的日志处理，等价于原 `aurora-scheduler` 中 `redisTimer` 黑名单。
- starter 默认使用 MyBatis-Plus 持久化。如果业务方使用其他 ORM（JPA / MyBatis 原生），可以自己写 Entity/Mapper/Service，仅依赖 `ScheduleManager` / `JobContext` / `JobLogHandler` 这套核心抽象。
