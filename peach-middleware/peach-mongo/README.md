# peach-mongo

[English](README.en-US.md) | 中文

`peach-mongo` 提供 MongoDB 自动配置和统一访问入口，业务通过 `peach-mongo-starter` 接入。公开入口是自动装配的 `IMongoService`（`MongoTemplate` 封装）。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo 配置、自动装配和 `IMongoService` 契约 |
| `peach-mongo-starter` | 业务接入依赖入口 |
| `peach-mongo-quickstart` | 审计日志场景下的最小能力验证 |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-mongo-starter</artifactId>
</dependency>
```

**Starter 运行依赖：** `peach-mongo-starter` 明确引入 MongoDB Spring Data 客户端；`PageInfo` 是公开返回类型，因此仅保留 PageHelper 核心依赖（6.1.0），不引入其 MyBatis Spring Boot Starter。仅引入 `peach-mongo-autoconfigure` 不保证拥有 MongoDB 客户端。缺少可用数据库时不能把运行时读写视为已通过验证。



最小配置：`peach.mongo.uri`；URI 未带库名时再配 `peach.mongo.database`。事务默认关闭（`peach.mongo.transaction.enabled=false`），开启需要副本集或分片集群。

## Quick Start

示例位于 [`peach-mongo-quickstart`](./peach-mongo-quickstart/)，无 Web 端口（`spring.main.web-application-type=none`）。以审计日志演示 `IMongoService`。

| 项 | 说明 |
| --- | --- |
| 能力样例 | **写入查询**：`insertOne` / `findList`；**更新**：`updateOne` + `$set`；**分页删除**：`insertMany` / `findPage` / `count` / `deleteOne` |
| Runner | `MongoDemoRunner`；关闭演示：`quickstart.mongo.demo.enabled=false` |
| 测试 | Testcontainers `mongo:7.0`；无 Docker 时 `disabledWithoutDocker=true` 跳过 |
| 前置 | 本地 Mongo 或 `PEACH_MONGO_URI` |

```bash
mvn -pl peach-middleware/peach-mongo/peach-mongo-quickstart -am spring-boot:run
mvn -pl peach-middleware/peach-mongo/peach-mongo-quickstart -am test
```

MongoDB URI、账号和密码只通过本地配置或环境变量提供。

**大结果集边界：** `peach.mongo.max-list-size` 默认 `500`，`peach.mongo.max-page-size` 默认 `500`，均要求 1～100000。未传 `limit` 的 `findList` 最多读取 `max-list-size + 1` 条用于判断是否超额；超额则抛异常而非静默截断。显式 `limit` 和 `findPage` 的 `pageSize` 超限会在访问数据库前拒绝。旧业务若需大批量数据必须改用分页/游标；不可将 `findList(collection)` 当作全表导出接口。单文档体积及聚合管线仍需业务限制。

**Mongo 命令日志安全边界：** 默认命令以 `?` 隐藏查询/更新参数，`Parameters` 仅输出最多 16 个 BSON 类型，不记录任何参数原值。慢查询、缺少过滤条件及命令失败的 WARN/ERROR **始终输出占位符命令**，即使调试时启用了 `peach.mongo.log.executable=true`；仅普通 DEBUG 可选择可执行命令（可能包含敏感值，生产环境应关闭）。`peach.mongo.log.max-command-length` 默认 4000、硬上限 16384，所有日志级别均截断，非正值也不能关闭限制。Inflight 日志缓冲限定 4096 条，通过有界插入顺序结构维护，不持有已结束命令。

## 边界

- Starter 不部署 MongoDB。
- Mongo URI、账号和密码必须来自安全配置来源，不写入 README 或源码。
- 通用访问接口不替代业务聚合查询、索引和数据生命周期设计。
- `updateOne` / `updateMany` 的更新文档必须使用 `$set` 等操作符。
- 文档结构演进需要明确旧数据兼容和迁移策略。
