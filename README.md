# Aurora PlatformBoot Starter

![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.5.0-blue.svg)
![JDK](https://img.shields.io/badge/JDK-21+-blue.svg)
![Version](https://img.shields.io/badge/Version-1.0.1--SNAPSHOT-blue.svg)
[![License](https://img.shields.io/badge/License-Apache%20License%202.0-B9D6AF.svg)](./LICENSE)
<br/>
[![Author](https://img.shields.io/badge/Author-wuhobin-green.svg)](https://github.com/wuhobin)
[![Copyright](https://img.shields.io/badge/Copyright-2026%20wuhobin%20%20@PlatformBootStarter-green.svg)](https://github.com/wuhobin)

基于 Spring Boot 3.5 / JDK 21 的多模块 Maven 父工程，目标是将底层基础设施（数据访问、鉴权、任务调度等）沉淀为可被业务工程依赖的自定义 Spring Boot Starter。

## 模块拓扑

```
platform-parent (pom)
├── platform-dependencies-bom         # 三方依赖版本集中管理（BOM）
├── platform-common                   # 通用工具/常量/分页/反射
└── platform-starter (pom)            # 所有自定义 starter 的聚合父模块
    ├── mybatis-plus-spring-boot-starter    # MyBatis-Plus 自动装配
    ├── knife4j-spring-boot-starter        # Knife4j 接口文档增强
    ├── redis-spring-boot-starter          # Redis 数据缓存（Lettuce + Redisson）
    ├── xlock-spring-boot-starter          # Redisson 分布式锁
    ├── sa-token-spring-boot-starter       # Sa-Token 多账号鉴权
    ├── oss-spring-boot-starter            # 对象存储（阿里云/腾讯云/MinIO 等）
    └── quartz-spring-boot-starter         # 通用 Quartz 定时任务调度

platform-example                     # 示例/自测工程，不在父 modules 中
```

## Starter 一览

| Starter | 说明 |
|------|------|
| **mybatis-plus** | MyBatis-Plus 自动装配（分页插件、自动填充、字段加密、动态表名） |
| **knife4j** | Knife4j 接口文档增强配置 |
| **redis** | Redis 缓存（布隆过滤器、两级缓存、消息队列、Pub/Sub、限流器、延时任务） |
| **xlock** | 分布式锁（可重入/公平/联锁/红锁/读写锁），支持注解 + 编程式两种方式 |
| **sa-token** | Sa-Token 多账号体系（Admin / Merchant / User），注解鉴权 + 路由拦截 |
| **oss** | 统一对象存储门面（阿里云 OSS / 腾讯云 COS / MinIO / 本地存储等），链式上传 API |
| **quartz** | 开箱即用的定时任务调度（建表 或 `auto-init-table: true` + 写 Controller 三步即可），`invokeTarget` 字符串反射调用 |

## 快速开始

```bash
# 安装父 POM 及所有发布模块到本地仓库
mvn -f pom.xml clean install -DskipTests

# 运行示例工程（先确保依赖的 starter 已 install 到本地仓库）
mvn -f platform-example/pom.xml spring-boot:run

# 仅构建某一个模块（-am 同时构建其依赖）
mvn -pl platform-starter/quartz-spring-boot-starter -am install -DskipTests
```

## 注意事项

- 版本号通过 `${revision}` 占位符统一注入，子模块 POM 不写死版本。
- `platform-example` 通过本地仓库依赖 starter，**改完 starter 必须重新 `install`** 才能在示例工程生效。
- 编译器开启了 `-parameters`，可依赖参数名反射。
