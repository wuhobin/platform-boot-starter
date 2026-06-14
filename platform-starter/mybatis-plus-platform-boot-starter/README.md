# mybatis-plus-platform-boot-starter

MyBatis-Plus 自动装配 Starter，提供动态条件构造和分页工具。

## 功能特性

- ✅ 动态条件构造（`@QueryField` 注解）
- ✅ 纯 MyBatis-Plus 原生分页（零 PageHelper 依赖）
- ✅ 支持两种排序格式：SQL 标准 / 后端约定（`-field`）
- ✅ 自动参数校验和 SQL 注入防护
- ✅ 分页结果 VO 转换

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>mybatis-plus-platform-boot-starter</artifactId>
</dependency>
```

## 分页工具使用

### 基础用法（推荐）

```java
@GetMapping("/users/page")
public Result<IPage<User>> page(UserQuery query, PageParam pageParam) {
    Page<User> page = PageUtils.buildPage(pageParam);
    Wrapper<User> wrapper = DynamicCondition.toWrapper(query);
    IPage<User> result = userMapper.selectPage(page, wrapper);
    return Result.data(result);
}
```

**前端传参：** `GET /users/page?page=2&size=20&orderBy=create_time desc,id asc`

### 排序格式

```java
// SQL 标准格式
Page<User> page = PageUtils.buildPage(1, 10, "create_time desc, id asc");

// 后端约定格式（- 表示 DESC，+ 表示 ASC）
Page<User> page = PageUtils.buildPage(1, 10, "-create_time,+id");
```

### VO 转换

```java
Page<UserVO> voPage = PageUtils.convert(entityPage, user -> {
    UserVO vo = new UserVO();
    vo.setId(user.getId());
    vo.setName(user.getName());
    return vo;
});
```

## PageUtils API

| 方法 | 说明 |
|------|------|
| `buildPage(PageParam)` | 从分页参数构造 Page（推荐） |
| `buildPage(pageNo, pageSize)` | 快速构造分页对象 |
| `buildPage(pageNo, pageSize, orderBy)` | 带排序的分页对象 |
| `convert(Page<S>, Function<S,T>)` | VO 转换，保留分页元信息 |

## 完整示例

参见 `platform-example` 模块的 `UserController`，包含 4 种分页用法：

1. **基础分页** - `/users/page`
2. **PageParam 自动处理** - `/users/page-with-param`
3. **VO 转换** - `/users/page-vo`
4. **简单分页** - `/users/page-simple`

## 注意事项

- orderBy 自动进行 SQL 注入校验
- page < 1 修正为 1，size > 2000 限制为 2000
- 排序字段自动转换为下划线格式
