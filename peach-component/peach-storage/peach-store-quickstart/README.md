# peach-store-quickstart

用于验证 `peach-store-starter` 的对象上传、下载、复制、移动、批量删除、分片上传和前端直传 token 接入。

## 接入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-store-starter</artifactId>
</dependency>
```

启动类为 `com.peach.quickstart.PeachStoreQuickstartApplication`，`StorageQuickstartRunner` 展示 `StorageTemplate` 的调用方式。具体 provider、桶和凭证配置从 `peach-store` README 及 provider 配置类确认，禁止把凭证写入源码。

## 运行

```bash
mvn -f peach-component/peach-storage/peach-store-quickstart/pom.xml spring-boot:run
```

运行前必须准备对应对象存储服务，并检查 capability 后再调用 provider 特有操作。quickstart 的对象 key 仅用于本地验证，生产业务应增加租户、目录和对象所有权校验。
