# peach-storage 模块参考

## 模块路径

- 聚合模块：`peach-component/peach-storage`
- 自动配置：`peach-component/peach-storage/peach-store-autoconfigure`
- 对外 starter：`peach-component/peach-storage/peach-store-starter`
- 示例：`peach-component/peach-storage/peach-store-example`

## 核心对象

- `StorageTemplate`：业务统一入口，负责默认 provider 和命名 provider 路由。
- `StorageProvider`：运行期 SPI，定义上传、下载、删除、列表、复制、移动、预签名、前端直传、分片等行为。
- `StorageProviderFactory`：启动期 SPI，定义 `storageType()`、`validate()`、`create()`。
- `StorageProviderRegistry`：管理 provider 集合和关闭。
- `StorageProperties`：绑定 `peach.storage` 配置。
- `StoragePathUtil`：路径规范化与越界保护。
- `StorageLogSanitizer`：日志脱敏。

## 配置基线

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

## provider 通用字段

- `type`：`LOCAL`、`NAS`、`SFTP`、`OSS`、`OBS`、`S3`、`MINIO`、`COS`、`BOS`、`CEPH`。
- `bucket-name`：对象存储真实 bucket；LOCAL/NAS/SFTP 中是逻辑 alias。
- `prefix`：provider 级对象 key 前缀。
- `endpoint`、`region`、`access-key`、`secret-key`：对象存储或 SFTP/NAS 连接参数。
- `root-path`：LOCAL/NAS/SFTP 的真实边界目录。
- `domain`：公开访问域名，只影响 URL 生成。
- `path-style-access`：S3/MinIO/Ceph 常用。
- `extra-properties`：provider 专属参数。

## 能力边界

- LOCAL/NAS/SFTP 可上传、下载、删除、列表、复制、移动，但预签名 URL 属于降级 URL，不代表访问控制凭证。
- 对象存储 provider 支持更完整的预签名、分片上传，前端直传能力以具体 provider 实现为准。
- NAS/SFTP 大文件下载当前需要特别关注内存和流式处理边界。
- `move` 默认可能是 copy + delete，不要承诺原子重命名。

## 构建验证

```bash
mvn -f "peach-component/peach-storage/pom.xml" test
mvn -f "peach-component/peach-storage/pom.xml" -DskipTests package
```
