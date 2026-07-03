# peach-storage

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-storage`  
类型：统一存储组件聚合模块

## 模块定位

`peach-storage` 提供统一存储入口，通过 `StorageTemplate` 屏蔽 LOCAL、NAS、SFTP、OSS、OBS、S3、MinIO、COS、BOS、Ceph 等 provider 差异。业务模块通过 `peach-store-starter` 接入，不直接调用厂商 SDK。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-store-autoconfigure` | 核心 API、请求响应模型、provider SPI、自动配置和默认实现 |
| `peach-store-starter` | 对业务模块暴露的 starter |
| `peach-store-example` | 可运行示例 |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `StorageTemplate` | 业务统一入口，支持默认 provider 和命名 provider |
| `StorageProvider` | 运行期存储 SPI |
| `StorageProviderFactory` | 启动期 provider 创建和校验 SPI |
| `StorageProviderRegistry` | provider 注册、查找和关闭 |
| `StorageProperties` | 绑定 `peach.storage` 配置 |
| `StoragePathUtil` | 对象 key 规范化和越界保护 |
| `StorageLogSanitizer` | 敏感日志脱敏 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-store-starter</artifactId>
</dependency>
```

最小配置：

```yaml
peach:
  storage:
    enabled: true
    primary: local
    providers:
      local:
        type: LOCAL
        bucket-name: app-local
        root-path: D:/data/peach-storage
        prefix: dev/app
        domain: http://localhost/files
```

## 使用示例

```java
@Resource
private StorageTemplate storageTemplate;

public UploadResult upload(UploadContent content) {
    return storageTemplate.upload(UploadObjectRequest.builder()
            .objectKey("docs/readme.txt")
            .content(content)
            .contentType(StorageContentType.TEXT_PLAIN_UTF8)
            .build());
}
```

指定 provider：

```java
storageTemplate.upload("archive", request);
storageTemplate.download("archive", downloadRequest);
```

## Provider 扩展

新增 provider 至少需要：

- 实现 `StorageProvider`。
- 实现 `StorageProviderFactory`。
- 在 `META-INF/services/com.peach.storage.spi.StorageProviderFactory` 注册 factory。
- 补齐 `StorageType`、配置校验和能力声明。
- 为路径校验、配置校验和主要操作补测试。

## 能力边界

- `objectKey` 是业务对象 key，统一使用 `/`，不要传本地绝对路径、URL 或包含 `..` 的越界路径。
- `copy`、`move`、`batchDelete` 不承诺分布式事务，调用方需要处理部分成功和补偿。
- LOCAL/NAS/SFTP 的预签名 URL 是降级 URL，不等同于对象存储访问控制凭证。
- 对象存储 provider 的前端直传、分片上传能力以 `StorageCapability` 和具体实现为准。
- 密钥、签名 URL、前端直传 token 不允许完整写入日志。

## 构建与验证

```bash
mvn -f "peach-component/peach-storage/pom.xml" test
mvn -f "peach-component/peach-storage/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-storage -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `StorageTemplate` 未注入 | 是否引入 `peach-store-starter`；`peach.storage.enabled` 是否开启 | 检查依赖和自动配置条件 |
| 启动时报 provider 配置错误 | `primary` 是否命中 `providers`；必填字段是否完整 | 对照 `StorageProperties` 补齐配置 |
| 路径越界异常 | `objectKey` 是否包含绝对路径或 `..` | 只传业务对象 key |
| 预签名或前端直传不可用 | provider 是否声明对应能力 | 检查 `StorageCapability` 和 provider 实现 |
| 批量删除部分失败 | provider 返回结果和业务补偿是否处理 | 记录失败对象并重试或人工处理 |
