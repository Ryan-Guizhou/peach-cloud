# peach-storage 模块参考

本文只记录当前源码可验证的入口、能力和边界。配置或 provider 变化后必须重新核对源码，不能把本文当作永久事实。

## 模块导航

```text
peach-component/peach-storage/
├── pom.xml                                # 聚合模块
├── README.md
├── peach-store-autoconfigure/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/peach/
│       │   ├── PeachStorageAutoConfiguration.java
│       │   ├── config/StorageProperties.java
│       │   └── storage/
│       │       ├── StorageTemplate.java   # 业务统一入口
│       │       ├── spi/                   # StorageProvider/Factory
│       │       ├── factory/               # provider 创建与校验
│       │       └── provider/              # LOCAL/OSS/S3 等实现
│       ├── main/resources/META-INF/services/
│       └── test/java/com/peach/storage/
├── peach-store-starter/
│   └── pom.xml                            # 业务接入依赖
└── peach-store-example/
    ├── pom.xml
    └── src/main/                          # 可运行接入示例
```

导航时忽略 `target/`、`.flattened-pom.xml` 等构建产物。

## 可验证入口

- `StorageTemplate`：默认 provider 和命名 provider 的统一路由入口。
- `StorageProvider`：运行期存储能力 SPI。
- `StorageProviderFactory`：启动期类型识别、配置校验和实例创建 SPI。
- `StorageProviderRegistry`：provider 注册与生命周期管理。
- `StorageProperties`：`peach.storage` 配置绑定。
- `StoragePathUtil`：对象 key/路径规范化和越界防护。
- `StorageLogSanitizer`：日志脱敏。

## REQUIRED

- 业务模块依赖 `peach-store-starter` 并调用 `StorageTemplate`，不得直接耦合厂商 SDK。
- `primary` 必须指向已配置 provider；objectKey 只能是业务对象 key，禁止绝对路径、URL 和 `..` 越界片段。
- 调用预签名、前端直传、分片等能力前检查 provider capability。
- secret、签名 URL、直传 token 和完整请求不得进入日志、异常或文档。
- provider 持有 client、连接池或线程资源时必须可关闭，并由 registry 统一释放。

## PREFERRED

- factory 负责启动期必填项、SDK 和类型约束；provider 负责运行期 I/O。
- 复制、移动、批量删除显式返回部分成功/失败，不伪装分布式事务。
- 大文件使用流式接口并明确 InputStream 所有权和关闭责任。

## LEGACY_COMPATIBLE

- `bucket-name` 在 LOCAL/NAS/SFTP 中作为逻辑 alias、预签名降级 URL 等现有语义只做兼容；新增文档必须明确不等于对象存储安全凭证。
- provider 类型和配置字段以 `StorageProperties`、factory `validate()` 和 capability 为准，不从示例反推。

## 配置示意

示例值只用于本地开发，不代表生产默认值：

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
```

## 验证

```bash
mvn -f "peach-component/peach-storage/pom.xml" test
mvn -f "peach-component/peach-storage/pom.xml" -DskipTests package
node scripts/check-utf8.mjs
```
