# peach-fileservice

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-fileservice`  
类型：文件业务域聚合模块

## 模块定位

`peach-fileservice` 提供文件领域服务、文件 REST 接口、文件实体模型和 OpenFeign 外部接口。它面向业务系统承接文件上传、下载、查询和存储记录管理，并可与 `peach-component/peach-storage` 的统一存储能力配合使用。

本模块解决：

- 文件领域模型、服务和接口分层。
- 文件服务启动入口。
- 内部文件 REST API。
- 面向其他服务的文件 OpenFeign 外部接口。

本模块不解决：

- 各云厂商对象存储 SDK 的统一封装，相关能力位于 `peach-component/peach-storage`。
- 生产文件生命周期治理、病毒扫描、内容审核、CDN 刷新等平台能力。
- 跨存储 provider 的分布式事务。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-fileservice-common` | 文件域公共对象和常量 |
| `peach-fileservice-entity` | 文件域实体、DTO、VO、查询对象 |
| `peach-fileservice-service` | 文件领域服务和数据访问 |
| `peach-fileservice-rest` | 文件 REST 接口 |
| `peach-fileservice-openfeign-external` | 文件服务 OpenFeign 外部接口 |
| `peach-fileservice-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-fileservice-launch/src/main/java/com/peach/fileservice/launch/PeachFileserviceApplication.java` |
| 配置文件 | `peach-fileservice-launch/src/main/resources/application-dev.yml` |
| REST 控制器 | `peach-fileservice-rest/src/main/java/com/peach/fileservice/rest/internal/FileController.java` |
| REST 前缀 | `/file/internal/` |
| 服务包 | `peach-fileservice-service/src/main/java/com/peach/fileservice/service` |
| OpenFeign | `peach-fileservice-openfeign-external/src/main/java/com/peach/fileservice/openfeign` |

## 运行机制

1. `peach-fileservice-launch` 加载当前 profile 并启动文件服务。
2. REST 层接收文件相关请求。
3. Service 层处理文件元数据、业务校验和存储交互。
4. 其他服务通过 OpenFeign 模块调用文件服务。
5. 如接入统一存储，实际文件读写由 `StorageTemplate` 和对应 provider 完成。

## 配置说明

- 启动配置位于 `peach-fileservice-launch/src/main/resources/application-*.yml`。
- 数据库、Nacos、Redis 和存储 provider 参数需要按环境配置。
- 如果使用 `peach-storage`，应同时检查 `peach.storage.primary` 与 `peach.storage.providers`。
- 不应在配置或文档中写入真实对象存储密钥、签名 URL 或生产 bucket。

## 边界与限制

- 文件服务可以管理文件业务记录，但文件物理存储的一致性取决于实际 provider。
- 删除、批量删除、迁移、复制等操作需要业务侧明确审计和补偿策略。
- 大文件上传、断点续传、前端直传和分片能力应以存储组件和 provider 能力为准。
- 文件访问权限不能只依赖 URL 隐蔽性，应结合认证、授权、签名和过期时间设计。

## 构建与验证

```bash
mvn -f "peach-fileservice/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-fileservice/peach-fileservice-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-fileservice/peach-fileservice-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 文件接口 404 | 网关路由、服务端口、REST 前缀是否正确 | 先直连文件服务，再经网关验证 |
| 上传失败 | 存储 provider 配置、目录权限、bucket 是否存在 | 检查 `peach.storage` 配置和 provider 日志 |
| 下载为空或 404 | 元数据与物理对象是否一致；objectKey 是否正确 | 检查数据库记录和实际存储对象 |
| Feign 调用失败 | 文件服务是否注册；调用方是否引入 external 模块 | 检查 Nacos、Feign 配置和服务名 |
| 生产泄密风险 | 日志是否打印签名 URL、access key、secret key | 对敏感字段做脱敏，避免提交真实密钥 |
