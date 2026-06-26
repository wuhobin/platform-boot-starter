# oss-spring-boot-starter

通用文件存储模块，基于 [x-file-storage](https://x-file-storage.xuyanwu.cn/) 封装。

## 快速开始

### 1. 引入依赖

```xml
<!-- OSS Starter -->
<dependency>
    <groupId>com.aurora</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
</dependency>

<!-- 按需引入云平台 SDK，例如阿里云 OSS -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.16.1</version>
</dependency>
```

### 2. 配置文件

保留 x-file-storage 原生 `dromara.x-file-storage` 前缀，与官方文档完全兼容：

```yaml
dromara:
  x-file-storage:
    default-platform: aliyun-oss-1
    aliyun-oss:
      - platform: aliyun-oss-1
        enable-storage: true
        access-key: ${OSS_ACCESS_KEY}
        secret-key: ${OSS_SECRET_KEY}
        end-point: oss-cn-shanghai.aliyuncs.com
        bucket-name: my-bucket
        domain: https://my-bucket.oss-cn-shanghai.aliyuncs.com/
        base-path: files/
```

### 3. 代码调用

```java
@RestController
public class FileController {

    @Autowired
    private OssTemplate ossTemplate;

    // 最简上传
    @PostMapping("/upload")
    public OssUploadResult upload(@RequestParam MultipartFile file) {
        return ossTemplate.upload(file);
    }

    // 快捷上传：指定路径和文件名
    @PostMapping("/upload-avatar")
    public OssUploadResult uploadAvatar(@RequestParam MultipartFile file) {
        return ossTemplate.upload(file, "avatars/", "user-123.jpg");
    }

    // 链式上传：复杂场景
    @PostMapping("/upload-full")
    public OssUploadResult uploadFull(@RequestParam MultipartFile file) {
        return ossTemplate.of(file)
                .setPath("documents/")
                .setPlatform("aliyun-oss-1")
                .putAttr("uploadBy", "admin")
                .upload();
    }

    // 上传图片并生成缩略图
    @PostMapping("/upload-image")
    public OssUploadResult uploadImage(@RequestParam MultipartFile file) {
        return ossTemplate.uploadImage(file, 1000, 1000);
    }

    // 下载
    @GetMapping("/download")
    public byte[] download(@RequestParam String url) {
        return ossTemplate.download(url);
    }

    // 删除
    @DeleteMapping("/delete")
    public boolean delete(@RequestParam String url) {
        return ossTemplate.delete(url);
    }
}
```

---

## 支持的云平台

### 云平台 SDK 坐标

| 平台 | Maven 坐标 | 推荐版本 |
|------|-----------|---------|
| 阿里云 OSS | `com.aliyun.oss:aliyun-sdk-oss` | 3.16.1 |
| 腾讯云 COS | `com.qcloud:cos_api` | 5.6.137 |
| 华为云 OBS | `com.huaweicloud:esdk-obs-java` | 3.22.12 |
| MinIO | `io.minio:minio` | 8.5.2 |
| Google Cloud Storage | `com.google.cloud:google-cloud-storage` | 2.20.1 |
| AWS S3 | 可通过 MinIO 兼容模式连接 | - |
| 本地存储 | 无需额外依赖（内置） | - |
| FTP | 无需额外依赖（内置） | - |
| SFTP | 无需额外依赖（内置） | - |
| WebDAV | 无需额外依赖（内置） | - |

> **注意**：Starter 不传递任何云 SDK。下游按需引入，只引入需要用到的平台 SDK。

### 各云平台配置示例

#### 阿里云 OSS

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.16.1</version>
</dependency>
```

```yaml
dromara:
  x-file-storage:
    default-platform: aliyun-oss-1
    aliyun-oss:
      - platform: aliyun-oss-1
        enable-storage: true
        access-key: ${ALI_ACCESS_KEY}
        secret-key: ${ALI_SECRET_KEY}
        end-point: oss-cn-shanghai.aliyuncs.com
        bucket-name: my-bucket
        domain: https://my-bucket.oss-cn-shanghai.aliyuncs.com/
        base-path: files/
```

#### 腾讯云 COS

```xml
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <version>5.6.137</version>
</dependency>
```

```yaml
dromara:
  x-file-storage:
    default-platform: tencent-cos-1
    tencent-cos:
      - platform: tencent-cos-1
        enable-storage: true
        secret-id: ${TENCENT_SECRET_ID}
        secret-key: ${TENCENT_SECRET_KEY}
        region: ap-guangzhou
        bucket-name: my-bucket-1234567890
        domain: https://my-bucket-1234567890.cos.ap-guangzhou.myqcloud.com/
        base-path: files/
```

#### 华为云 OBS

```xml
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java</artifactId>
    <version>3.22.12</version>
</dependency>
```

```yaml
dromara:
  x-file-storage:
    default-platform: huawei-obs-1
    huawei-obs:
      - platform: huawei-obs-1
        enable-storage: true
        access-key: ${HUAWEI_ACCESS_KEY}
        secret-key: ${HUAWEI_SECRET_KEY}
        end-point: obs.cn-south-1.myhuaweicloud.com
        bucket-name: my-bucket
        domain: https://my-bucket.obs.cn-south-1.myhuaweicloud.com/
        base-path: files/
```

#### MinIO

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
```

```yaml
dromara:
  x-file-storage:
    default-platform: minio-1
    minio:
      - platform: minio-1
        enable-storage: true
        access-key: minioadmin
        secret-key: minioadmin
        end-point: http://localhost:9000
        bucket-name: test
        domain: http://localhost:9000/test/
        base-path: files/
```

#### 本地存储

无需额外依赖。

```yaml
dromara:
  x-file-storage:
    default-platform: local-1
    local:
      - platform: local-1
        enable-storage: true
        base-path: /data/uploads/
        domain: http://localhost:8080/file/
```

#### FTP

无需额外依赖。

```yaml
dromara:
  x-file-storage:
    default-platform: ftp-1
    ftp:
      - platform: ftp-1
        enable-storage: true
        host: ftp.example.com
        port: 21
        user: ftpuser
        password: ftppass
        domain: ftp://ftp.example.com/
        base-path: /uploads/
```

#### SFTP

无需额外依赖。

```yaml
dromara:
  x-file-storage:
    default-platform: sftp-1
    sftp:
      - platform: sftp-1
        enable-storage: true
        host: sftp.example.com
        port: 22
        user: sftpuser
        password: sftppass
        domain: sftp://sftp.example.com/
        base-path: /home/uploads/
```

#### WebDAV

无需额外依赖。

```yaml
dromara:
  x-file-storage:
    default-platform: webdav-1
    webdav:
      - platform: webdav-1
        enable-storage: true
        server: https://webdav.example.com
        user: webdavuser
        password: webdavpass
        domain: https://webdav.example.com/
        base-path: /files/
```

---

## OssTemplate API 参考

### 快捷上传

| 方法 | 说明 |
|------|------|
| `upload(MultipartFile file)` | 最简上传 |
| `upload(File file)` | 上传本地文件 |
| `upload(byte[] bytes, String originalFilename)` | 上传字节数组 |
| `upload(InputStream in, String originalFilename)` | 上传输入流 |
| `upload(MultipartFile file, String path)` | 指定路径上传 |
| `upload(MultipartFile file, String path, String saveFilename)` | 指定路径和文件名 |
| `upload(MultipartFile file, String path, String saveFilename, String platform)` | 指定平台上传 |
| `uploadImage(MultipartFile file, int width, int height)` | 上传图片并缩放+缩略图 |

### 链式上传

| 方法 | 说明 |
|------|------|
| `of(MultipartFile file)` | 返回 OssUploadBuilder |
| `of(File file)` | 返回 OssUploadBuilder |
| `of(byte[] bytes, String originalFilename)` | 返回 OssUploadBuilder |
| `of(InputStream in, String originalFilename)` | 返回 OssUploadBuilder |

### OssUploadBuilder API

| 方法 | 说明 |
|------|------|
| `setPath(String path)` | 设置保存路径 |
| `setSaveFilename(String filename)` | 设置保存文件名 |
| `setPlatform(String platform)` | 指定存储平台 |
| `setObjectId(String objectId)` | 关联对象 ID |
| `setObjectType(String objectType)` | 关联对象类型 |
| `putAttr(String key, Object value)` | 自定义属性 |
| `setThumbnailSuffix(String suffix)` | 缩略图后缀 |
| `image(int width, int height)` | 缩放原图 |
| `thumbnail(int width, int height)` | 生成缩略图 |
| `upload()` | 执行上传返回 OssUploadResult |
| `uploadRaw()` | 执行上传返回原生 FileInfo |

### 下载

| 方法 | 说明 |
|------|------|
| `download(String url)` | 下载为 byte[] |
| `download(FileInfo fileInfo)` | 下载为 byte[] |
| `download(String url, OutputStream out)` | 下载到 OutputStream |
| `download(String url, Consumer<InputStream> consumer)` | 消费 InputStream |

### 删除

| 方法 | 说明 |
|------|------|
| `delete(String url)` | 通过 URL 删除 |
| `delete(FileInfo fileInfo)` | 通过 FileInfo 删除 |

### 文件信息

| 方法 | 说明 |
|------|------|
| `getFileInfo(String url)` | 获取文件信息 |
| `exists(String url)` | 判断文件是否存在 |

### 复制/移动

| 方法 | 说明 |
|------|------|
| `copy(String sourceUrl, String targetPath, String targetFilename)` | 同平台复制 |
| `copy(String sourceUrl, String targetPath, String targetFilename, String platform)` | 指定平台复制 |
| `move(String sourceUrl, String targetPath, String targetFilename)` | 同平台移动/重命名 |
| `move(String sourceUrl, String targetPath, String targetFilename, String platform)` | 指定平台移动 |
| `isSupportCopy(String platform)` | 检查平台是否支持复制 |
| `isSupportMove(String platform)` | 检查平台是否支持移动 |

### 原生服务

| 方法 | 说明 |
|------|------|
| `getFileStorageService()` | 获取底层 FileStorageService |

---

## OssUploadResult 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | 文件访问 URL |
| `filename` | String | 文件名 |
| `size` | Long | 文件大小（字节） |
| `platform` | String | 存储平台标识 |
| `thUrl` | String | 缩略图 URL（如有） |

---

## 多平台配置

可以同时配置多个存储平台，通过 `default-platform` 指定默认平台。运行时可通过 `setPlatform()` 或 `upload(file, path, filename, platform)` 覆盖。

```yaml
dromara:
  x-file-storage:
    default-platform: aliyun-oss-1
    aliyun-oss:
      - platform: aliyun-oss-1
        enable-storage: true
        # ... 生产环境配置
      - platform: aliyun-oss-backup
        enable-storage: true
        # ... 备份环境配置
    local:
      - platform: local-dev
        enable-storage: true
        base-path: ./uploads/
        domain: http://localhost:8080/file/
```

## 高级用法

需要 x-file-storage 完整功能（如上传进度监听、分片上传自定义配置、直接操作 FileRecorder 等）时，直接注入原生的 `FileStorageService`：

```java
@Autowired
private FileStorageService fileStorageService;

// 使用原生 API 的全部能力
```
