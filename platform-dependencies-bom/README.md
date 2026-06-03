# cloud-dependencies-bom

## 模块简介

项目依赖版本管理模块（Bill of Materials），集中管理整个项目的依赖版本。

## 主要功能

### 统一版本管理
- 管理所有子模块的版本号
- 管理第三方依赖的版本号
- 确保项目依赖版本一致性

### 包含内容

#### 子模块版本
- cloud-common
- cloud-core
- cloud-component
- cloud-dal
- cloud-dal-mongo
- cloud-generator
- cloud-webmvc
- 各类 cloud-starter 模块

#### 第三方依赖版本
- Spring Boot/Cloud 版本
- MyBatis Plus 版本
- Redisson 版本
- Druid 版本
- 各种第三方SDK版本

## 使用方式

在项目的 `pom.xml` 中引入：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.cloud</groupId>
            <artifactId>cloud-dependencies-bom</artifactId>
            <version>${project.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

引入后，子模块引用依赖时无需指定版本号：

```xml
<dependency>
    <groupId>com.cloud</groupId>
    <artifactId>cloud-common</artifactId>
    <!-- 无需指定版本 -->
</dependency>
```