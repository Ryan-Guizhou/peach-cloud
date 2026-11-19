# peach-fileservice

[English](README.en-US.md) | 中文

最后更新时间：2026-08-12
运行基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0

## 模块定位

`peach-fileservice` 是文件领域服务，负责文件记录、对象引用、上传会话和对象存储调用之间的编排。底层存储能力来自 `peach-store-starter` 提供的 `MultiZoneStorage`。

模块提供：

- 内部文件管理接口：上传预检查、普通上传、分片上传、详情、下载 URL、分页、逻辑删除和恢复。
- 内部存储治理接口：存储实例配置、连通性测试、对象浏览和对象管理。
- 外部服务间接口：面向其他服务的业务文件上传、详情、下载 URL、逻辑删除和 SHA-256 辅助计算。
- OpenFeign 契约：`peach-fileservice-openfeign-external` 中的 `FileFeignClient`。
- 定时清理能力：过期逻辑删除文件、过期上传会话。

模块不提供：

- 云厂商 SDK 通用封装；provider 能力属于 `peach-component/peach-storage`。
- 文件业务权限、租户隔离、下载审计、CDN 刷新、内容审核或病毒扫描闭环。
- 数据库和对象存储之间的分布式事务。
- 对外暴露 bucket、objectKey、本地路径或调用方机器路径操作。

## 模块结构

```text
peach-fileservice/
├── peach-fileservice-common/              # 文件域常量和工具
├── peach-fileservice-entity/              # DO、DTO、QO、VO
├── peach-fileservice-service/             # 领域服务、DAO/XML、清理任务
├── peach-fileservice-rest/                # internal/external REST 控制器
├── peach-fileservice-openfeign-external/  # FileFeignClient 和自动配置
├── peach-fileservice-launch/              # 启动模块和运行配置
├── pom.xml
├── README.md
└── README.en-US.md
```

相关表脚本位于仓库根目录：

- `sql/PEACH_FILE_OBJECT.sql`
- `sql/PEACH_FILE_RECORD.sql`
- `sql/PEACH_FILE_UPLOAD_SESSION.sql`

## 接口归置

### Internal：前端/管理端/本服务内部能力

Internal 接口只面向文件服务自身的管理能力和受控业务后台，不作为跨服务公共契约。

| 控制器 | 路径前缀 | 职责 |
| --- | --- | --- |
| `FileController` | `/file/internal` | 普通上传、预检查、文件记录查询、下载 URL、分页、逻辑删除、恢复 |
| `FileMultipartController` | `/file/internal/multipart` | 分片上传初始化、分片 URL、完成、中止 |
| `FileToolController` | `/file/internal/tools` | 文件摘要等内部工具 |
| `CloudStorageInstanceController` | `/file/internal/storage/instance` | 存储实例配置管理、启停、连通性测试 |
| `CloudStorageBrowserController` | `/file/internal/storage/browser` | 对象存储浏览、对象上传、目录和对象操作 |
| `HealthController` | `/file/health` | 服务健康检查 |

### External：服务间稳定契约

External 接口只暴露业务文件能力，路径前缀为 `/file/external`，由 `FileExternalController` 和 `FileFeignClient` 对齐维护。

| 方法 | 路径 | Feign 方法 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/file/external/upload` | `upload` | 服务端计算 SHA-256 后上传业务文件 |
| `POST` | `/file/external/tools/sha256` | `sha256` | 只计算摘要，不写对象存储 |
| `GET` | `/file/external/{fileId}` | `detail` | 返回脱敏文件详情 |
| `GET` | `/file/external/{fileId}/url` | `getUrl` | 返回临时下载 URL |
| `DELETE` | `/file/external/{fileId}` | `delete` | 按业务文件 ID 逻辑删除 |

External 不接受 `bucketName`、`objectKey`、`targetPath`、`localPath`、`sourceDir`、`localDir` 等存储定位或本地路径参数。

## 快速接入

调用方引入：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-fileservice-openfeign-external</artifactId>
    <version>${revision}</version>
</dependency>
```

上传示例：

```java
Response response = fileFeignClient.upload(
        file,
        "avatar",
        userId,
        "profile",
        "avatar.png",
        "image/png",
        null,
        null
);
```

文件服务运行前需要准备 Nacos、数据源、Sa-Token、OpenFeign 和 `peach-store` provider 配置。`peach.file` 配置来自 `FileDomainProperties`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.file.default-provider` | 未声明 | 默认存储 provider |
| `peach.file.object-key-prefix` | `files` | 生成 objectKey 的前缀 |
| `peach.file.retention-days` | `30` | 逻辑删除保留天数 |
| `peach.file.download-url-expire-seconds` | `3600` | 下载预签名 URL 有效期 |
| `peach.file.part-url-expire-seconds` | `900` | 分片上传 URL 有效期 |
| `peach.file.upload-session-expire-minutes` | `120` | 上传会话过期时间 |
| `peach.file.cleanup-enabled` | `true` | 是否清理过期逻辑删除文件 |
| `peach.file.cleanup-cron` | `0 0 3 * * ?` | 文件清理 Cron |
| `peach.file.upload-session-cleanup-enabled` | `true` | 是否清理过期上传会话 |
| `peach.file.upload-session-cleanup-cron` | `0 0/30 * * * ?` | 会话清理 Cron |

## 运行机制

普通上传：

```text
调用方/前端
  -> FileController 或 FileExternalController
  -> IFileDomainService
  -> SHA-256 + fileSize 校验
  -> 查询可复用 FileObject
  -> MultiZoneStorage 上传或复用
  -> 写入 PEACH_FILE_OBJECT / PEACH_FILE_RECORD
```

分片上传：

```text
init
  -> part-url
  -> 调用方直传对象存储
  -> complete
  -> 服务端校验摘要和大小
  -> 写入对象和文件记录
```

删除是逻辑删除。物理删除由清理任务在保留期后处理；数据库事务不能回滚已经写入对象存储的对象，生产环境需要监控和补偿数据库/对象存储不一致。

## 安全边界

- `/file/internal/**` 和 `/file/external/**` 都必须接入认证、授权和网关路由控制。
- External 只返回脱敏后的文件信息，不返回 bucket、objectKey、provider 内部定位。
- 预签名 URL 属于临时凭据，不要写入日志、异常、审计记录或业务持久表。
- 存储实例配置可能包含 endpoint、accessKey、secretKey 等敏感信息，审计日志只能记录实例 ID、实例名等非敏感字段。
- 普通上传会读取完整 `MultipartFile` 字节，大文件优先使用分片直传。
- 当前 ClamAV 客户端未接入 `FileDomainServiceImpl` 上传链路，不能宣称已完成病毒扫描。

## 构建与验证

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -pl peach-fileservice/peach-fileservice-launch -am -DskipTests package -Pdevelopment
```

Maven 命令只验证编译和打包，不替代真实 provider、分片直传、权限和清理任务联调。

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Internal 接口 404 | 是否访问 `/file/internal/**` 新路径 | 核对网关路由和前端 API |
| Feign 404 | `FileFeignClient` 路径是否与 `/file/external/**` 一致 | 对齐 `FileApiConstant` 和服务端 Controller |
| `MultiZoneStorage` 注入失败 | 是否引入 `peach-store-starter`，provider 是否配置 | 检查 Nacos 和启动日志 |
| 摘要或大小校验失败 | `sha256` 和 `fileSize` 是否来自同一文件内容 | 重新计算摘要并核对 multipart 字段 |
| 分片 complete 失败 | session 是否过期，ETag/partNumber 是否完整 | 重新初始化会话并按返回 URL 上传 |
| 下载 URL 失效 | 是否超过 `download-url-expire-seconds` | 重新获取临时 URL |
| 删除后仍可恢复 | 是否仍在 `retention-days` 内 | 属于逻辑删除语义，检查清理任务状态 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。

### #231 文件详情与上传会话的现有对象边界收敛

此前文件分页由 `AuditContext.requireTenantId/requireOrgId` 赋予查询范围，但 `FileDomainServiceImpl.selectByFileId`、`requireRecord` 和 `requireSession` 没有统一验证数据库行归属，知道别人的文件 ID/上传会话 ID 时存在跨租户/机构访问风险。现由 `FileRecordScopeGuard` 在**返回详情、生成临时下载 URL、逻辑删除/恢复、读取上传会话**前比较服务端现有可信业务租户及机构与数据库记录的真实 `TENANT_ID/ORG_ID`。文件详情 SQL 补出归属列，任何上下文缺失、记录归属缺失或不同均拒绝。后台清理任务独立于用户会话，不通过模拟用户绕过校验。对应纯函数单测涵盖跨租户、跨机构和空范围。

**限制：** 这里仍是旧业务租户/机构号止血，不能替代新 SaaS 的 `tenantRefId/memberRefId/appRefId` 和行级/字段级真实授权，也不表示旧用户级 Redis Profile 已可靠迁移。后续完整物理迁移与所有 SQL/对象存储权限端到端覆盖另见 #232、#211；没有经过真实迁移与验收前不得当作生产隔离已完成。

额外在 `FileRecordDao.logicalDelete` / `restoreByFileId` 的 MyBatis `WHERE` 中绑定已校验记录的 `TENANT_ID` 与 `ORG_ID`，并检查影响行数必须为 1；并发期间归属或逻辑删除状态发生变化时抛错，避免只在 Java 查询时检查、SQL 写入却又退化为仅使用 `FILE_ID`。

### #232 物理 SaaS 文件隔离（研发中，尚未完成数据策略验收）

**已提交到 feature 分支的基础能力：** 文件记录、对象和上传会话统一增加 UUID 物理 `ID`，保留唯一业务编号 `FILE_ID`、`OBJECT_ID`、`SESSION_ID`；添加受信 `TENANT_REF_ID`、`APP_REF_ID`、`OWNER_USER_REF_ID`、可选 `ORG_REF_ID` 和上传者 `MEMBER_REF_ID`。通过 `SaasIamFeignClient` 在线向 Auth 校验实际最终用户 Token 的 API 动作与物理上下文，由 Auth 再校验成员、授权和全局密码版本。任意 Feign/Redis/同令牌验证失败都**拒绝**，不会降级使用浏览器自报业务租户或旧 USER_PROFILE。

- 秒传对象 SQL 同时限定物理 tenant、app 和 owner，禁止跨租户/用户借 SHA-256 复用对象；文件分页 SQL 与 COUNT 同时限定物理范围和 owner；文件详情、签名 URL 生成前必须从受限 SQL 读取真实记录。删除和恢复 UPDATE WHERE 再次约束三元组，上传会话续传需匹配上传成员物理 ID。
- 普通文件列表目前采取**仅所有者可见**的最严格兜底策略，不会假装已有全机构/跨机构共享能力。Role/Data/Field 按同一授权绑定求交的文件完整接入仍是 #232 尚未完成部分；在补齐之前，严禁将此阶段单独合并为生产完整 RBAC。
- `sql/PEACH_FILE_SAAS_PHYSICAL_MIGRATION.sql` 是**存量库一次性迁移**，需 DBA 备份、执行物理主键切换、做存量对象与真实 tenant/app/owner/member 的人工证据映射和核对。没有真实映射的旧行继续保持 NULL 并由新 SQL 过滤，不允许自动猜测、默认开放或把旧 TENANT_ID 当物理主键。新库用 `sql/PEACH_FILE_*.sql` / `sql/ALL_TABLE_CREATE.sql`，不得再次运行 ALTER。
- `sql/PEACH_FILE_SAAS_API_PERMISSIONS.sql` 只注册固定 SYSTEM API 定义，不自动授权。认证服务原有 `GET:/api/file/internal/*` 等通配资源不满足最新 API 路径解析器，应停止使用并按 `{fileId}` 模板配置最小授权。External 代理调用复用 Internal 的文件领域方法，因此当前需要同时具有相应内部动作权限。
- 文件服务内部清理任务仅访问已拥有物理范围映射的到期数据，不伪造终端用户 Token；历史未映射记录不得直接清理。
- 真实 MySQL、网关、Auth Feign Same-Token、Nacos/Redis、数据范围、字段遮盖、存储 provider 与迁移结果还需由 #211 验收。**本节仅描述代码进度，不代表 #232 已完成或可部署生产。**

