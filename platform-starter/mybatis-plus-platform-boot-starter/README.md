# mybatis-plus-platform-boot-starter

> 基于 MyBatis-Plus 3.5.8 的 Spring Boot 3 Starter，开箱即用。内置乐观锁、全表更新阻断（始终开启）、分页、动态表名（按月分表）、全表扫描拦截、自动填充以及基于注解的动态 `QueryWrapper` 构造能力。

![JDK](https://img.shields.io/badge/JDK-21+-blue.svg)
![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.5.0-blue.svg)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.8-blue.svg)

---

## 一、引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>mybatis-plus-platform-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> 该 starter 依赖 `common-platform-boot-starter`、`mybatis-plus-spring-boot3-starter`、`druid-spring-boot-3-starter`，会自动随依赖引入，无需重复声明。

数据源由业务侧自行配置（推荐 Druid）：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ******
    type: com.alibaba.druid.pool.DruidDataSource
```

---

## 二、自动装配内容

| Bean | 作用 | 说明 |
| --- | --- | --- |
| `OptimisticLockerInnerInterceptor` | 乐观锁，配合 `@Version` 字段使用 | 始终开启 |
| `BlockAttackInnerInterceptor` | 阻断无 where 的全表 update/delete | 始终开启 |
| `PaginationInnerInterceptor` | MyBatis-Plus 分页 | 始终开启 |
| `FullScanInterceptor` | 拦截 `selectList` 无条件全表扫描 | 通过 `disable-full-scan-table` 配置 |
| `DynamicTableNameInnerInterceptor` | 动态表名（按月分表） | 通过 `dynamic-table.enable` 开启 |
| `MetaObjectHandlerAdapter` | `BaseEntity` 子类自动填充 `createTime/updateTime` | `@ConditionalOnMissingBean`，可覆盖 |

---

## 三、配置项

以 `mybatis-plus.ext` 为前缀：

```yaml
mybatis-plus:
  ext:
    # 禁止无条件全表扫描的表（命中即抛 BadSqlGrammarException）
    disable-full-scan-table:
      - t_order
      - t_user

    # 动态表名配置
    dynamic-table:
      enable: false
      tables:
        - t_log
      # 线程未指定后缀时，是否回退到当前月份（yyyyMM）作为后缀。默认 false（回退原表名，不加后缀）
      fallback-to-current-month: false
```

---

## 四、基础用法

### 4.1 实体类继承 `BaseEntity`

`BaseEntity` 提供 `createTime`、`updateTime`（由 `MetaObjectHandlerAdapter` 自动填充）及透传用的 `params` map。

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer age;

    @Version
    private Integer version;
}
```

### 4.2 查询对象继承 `BaseQuery`

`BaseQuery` 内置常用查询字段：

| 字段 | 操作符 | 说明 |
| --- | --- | --- |
| `ids` | `IN` | 按 id 集合查询 |
| `createStartTime` | `>=` | 创建时间起 |
| `createEndTime` | `<` | 创建时间止 |
| `updateStartTime` | `>=` | 更新时间起 |
| `updateEndTime` | `<` | 更新时间止 |
| `limitSize` | `LIMIT N` | 限制返回数量 |

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends BaseQuery {

    /** 默认 EQ；field 不写时自动取字段名（驼峰转下划线） */
    @QueryField
    private String name;

    /** 模糊查询 */
    @QueryField(operator = Operator.LIKE)
    private String nickname;

    /** 多字段 OR：(name LIKE ? OR phone LIKE ? OR email LIKE ?) */
    @QueryField(operator = Operator.LIKE, orFields = {"name", "phone", "email"})
    private String keyword;

    /** 范围查询，支持四种开闭组合 */
    @QueryField(operator = Operator.BETWEEN, field = "age")
    private BetweenQueryAttribute<Integer> ageRange;
}
```

### 4.3 调用 `DynamicCondition` 生成 Wrapper

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public List<User> list(UserQuery query) {
        return userMapper.selectList(DynamicCondition.toWrapper(query));
    }

    /** 带排序 */
    public List<User> listSorted(UserQuery query, SortBy sort) {
        return userMapper.selectList(DynamicCondition.toWrapper(query, sort));
    }
}
```

> `DynamicCondition` 反射查询对象的非 `null` 字段，结合 `@QueryField` 构建 `QueryWrapper`，字段名自动驼峰转下划线，并跳过 `PageParam` 中声明的字段。

---

## 五、`@QueryField` 支持的操作符

| Operator | 等价 SQL | 说明 |
| --- | --- | --- |
| `EQ` / `NE` | `= / <>` | 默认操作符 |
| `LIKE` / `LIKE_LEFT` / `LIKE_RIGHT` / `NOT_LIKE` | `LIKE '%v%'` 等 | 全模糊/左模糊/右模糊 |
| `GT` / `LT` / `GTE` / `LTE` | `> < >= <=` | |
| `BETWEEN` / `NOT_BETWEEN` | 开闭区间 | 类型必须为 `BetweenQueryAttribute<?>` |
| `IS_NULL` / `NOT_NULL` | `IS NULL / IS NOT NULL` | |
| `IS_EMPTY` / `IS_AND_EMPTY` / `NOT_EMPTY` / `NOT_AND_EMPTY` | 空串与 null 组合 | |
| `IN` / `NOT_IN` | `IN / NOT IN` | 支持 `Collection`、数组、单值 |
| `FIND_IN_SET` | `FIND_IN_SET(?, field)` | |
| `GROUP` | `GROUP BY` | 多字段放 `fields = {...}` |
| `DISTINCT` | `SELECT DISTINCT` | 多字段放 `fields = {...}` |
| `LIMIT` | `LIMIT n` | 值类型为 `Integer`/`Long` |
| `JSON_ARRAY_ALL_MATCH` / `JSON_ARRAY_ANY_MATCH` 等 | `JSON_CONTAINS` | 含 Doris 变体 |

### 注解参数

```java
@QueryField(
    operator   = Operator.LIKE,          // 操作符，默认 EQ
    field      = "user_name",           // 数据库列名，默认取字段名
    orFields   = {"name", "alias"},     // 多字段 OR 查询
    fields     = {"a", "b"},            // GROUP/DISTINCT 多字段
    queryEmpty = false,                 // 空串是否作为条件，默认 false
    ignore     = false                  // 忽略该字段，不参与条件构造
)
```

### `BETWEEN` 开闭组合

配合 `BetweenQueryAttribute` 使用 `BetweenType` 枚举：

```java
// 左开右开 (end > x > start)
new BetweenQueryAttribute<>(start, end);
// 左闭右闭 (end >= x >= start)
new BetweenQueryAttribute<>(BetweenType.BOTH_EQUAL, start, end);
// 左闭右开 (end >= x > start)
new BetweenQueryAttribute<>(BetweenType.ONLY_MIN_EQUAL, start, end);
// 左开右闭 (end > x >= start)
new BetweenQueryAttribute<>(BetweenType.ONLY_MAX_EQUAL, start, end);
```

---

## 六、分页查询

`PaginationInnerInterceptor` 始终注册，直接使用 `IPage`：

```java
public IPage<User> page(UserQuery query, long pageNo, long pageSize) {
    return userMapper.selectPage(
        new Page<>(pageNo, pageSize),
        DynamicCondition.toWrapper(query)
    );
}
```

> `PageParam`（`common-platform-boot-starter`）可作为接入层入参，通过 `PageParam.getSort()` 转成 `SortBy` 传入 `DynamicCondition.toWrapper(query, sort)`。

---

## 七、全表扫描拦截

`FullScanInterceptor` 仅作用于 `BaseMapper.selectList`。判定规则：

1. SQL 含 `limit` / `count(0)` —— 放行
2. 方法标注 `@InterceptorIgnore(blockAttack="true")` —— 放行
3. where 为空、或仅有 `1=1`、或仅命中逻辑删除字段：
   - 命中 `disable-full-scan-table` → 抛 `BadSqlGrammarException`
   - 否则仅打印 warn 日志

```java
// 在禁止全表扫描的表中绕过拦截（确有需求时）
@InterceptorIgnore(blockAttack = "true")
List<User> selectAllWithoutCondition();
```

---

## 八、动态表名（按月分表）

```yaml
mybatis-plus:
  ext:
    dynamic-table:
      enable: true
      tables:
        - t_log
```

行为：

- 线程未指定后缀 → **默认回退原表名**（`fallback-to-current-month=false`）或当前月份（`true`）
- 手动指定后缀：通过 `RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, "202601")`

```java
try {
    RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, "202601"); // 查 t_log_202601
    return logMapper.selectList(wrapper);
} finally {
    RequestThread.clear(); // 必须清理，防止 ThreadLocal 泄漏
}
```

若后缀为 `Constants.DYNAMIC_TABLE_DEFAULT_NAME`（`"DEFAULT"`），则不加后缀。

> `RequestThread` 是普通 `ThreadLocal`，业务侧需在请求边界（过滤器/拦截器/MQ 消费/线程池任务）调用 `clear()`。

---

## 九、扩展点：`CustomInterceptor`

注入一个 `CustomInterceptor` Bean 即可追加自定义 `InnerInterceptor`：

```java
@Configuration
public class MybatisExtConfig {

    @Bean
    public CustomInterceptor tenantInterceptor() {
        return mp -> mp.addInnerInterceptor(new TenantLineInnerInterceptor(new MyTenantHandler()));
    }
}
```

支持多个 Bean，按 `@Order` 顺序追加。

---

## 十、覆盖默认 `MetaObjectHandler`

`MetaObjectHandlerAdapter` 仅对 `BaseEntity` 子类填充 `createTime/updateTime`。自定义 `MetaObjectHandler` Bean 可通过 `@ConditionalOnMissingBean` 自动覆盖：

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override public void insertFill(MetaObject mo) { /* 追加 creatorId / tenantId 等 */ }
    @Override public void updateFill(MetaObject mo) { /* ... */ }
}
```

---

## 十一、版本与依赖

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.0 |
| MyBatis-Plus | 3.5.8 |
| mybatis-spring | 3.0.4 |
| Druid (spring-boot-3-starter) | 1.2.25 |
| Hutool | 5.8.38 |