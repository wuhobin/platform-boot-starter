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

## Maven Central 坐标

已发布至 [Maven Central](https://central.sonatype.com/namespace/io.github.wuhobin)，`groupId` 为 `io.github.wuhobin`，版本号通过 BOM 统一管理。

### 引入 BOM（推荐）

在业务工程的 `pom.xml` 中导入平台 BOM，管理所有 starter 版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.wuhobin</groupId>
            <artifactId>platform-dependencies-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

之后引用各 starter 无需写版本号：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.wuhobin</groupId>
        <artifactId>quartz-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.wuhobin</groupId>
        <artifactId>mybatis-plus-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 继承 parent

如果业务工程需要复用平台 POM 的公共配置（JDK 21、`-parameters` 编译参数、Lombok、Maven 插件等），可继承 `platform-parent`：

```xml
<parent>
    <groupId>io.github.wuhobin</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.0.0</version>
    <relativePath/>
</parent>
```

> `platform-parent` 内部已通过 `<dependencyManagement>` 导入 `platform-dependencies-bom`，继承后业务方**不需**再单独引入 BOM。

### 完整示例

以下是一个使用 MyBatis-Plus + Quartz 定时任务 + Knife4j 接口文档的业务工程 POM：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 platform-parent，自动获得 JDK 21、Lombok、编译插件等配置 -->
    <parent>
        <groupId>io.github.wuhobin</groupId>
        <artifactId>platform-parent</artifactId>
        <version>1.0.0</version>
        <relativePath/>
    </parent>

    <artifactId>my-business-app</artifactId>
    <name>My Business App</name>

    <dependencies>
        <!-- Quartz 定时任务 -->
        <dependency>
            <groupId>io.github.wuhobin</groupId>
            <artifactId>quartz-spring-boot-starter</artifactId>
        </dependency>

        <!-- MyBatis-Plus 数据访问 -->
        <dependency>
            <groupId>io.github.wuhobin</groupId>
            <artifactId>mybatis-plus-spring-boot-starter</artifactId>
        </dependency>

        <!-- Knife4j 接口文档 -->
        <dependency>
            <groupId>io.github.wuhobin</groupId>
            <artifactId>knife4j-spring-boot-starter</artifactId>
        </dependency>

        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

> 所有 `io.github.wuhobin` 的依赖都**不需要写版本号**（由 `platform-parent` 通过 `platform-dependencies-bom` 统一管理）。

### 各模块独立坐标

如果不想用 BOM，可以逐个引入（需要写版本号）：

| 模块 | artifactId |
|------|------|
| 公共基础 | `platform-common` |
| Web MVC 通用件 | `platform-webmvc` |
| MyBatis-Plus | `mybatis-plus-spring-boot-starter` |
| Knife4j | `knife4j-spring-boot-starter` |
| Redis 缓存 | `redis-spring-boot-starter` |
| 分布式锁 | `xlock-spring-boot-starter` |
| Sa-Token | `sa-token-spring-boot-starter` |
| 对象存储 | `oss-spring-boot-starter` |
| 定时任务 | `quartz-spring-boot-starter` |
