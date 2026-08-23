# peach-scheduled

[English](README.en-US.md) | 中文

- 最后更新时间：2026-08-14
- artifactId：`peach-scheduled`
- 类型：定时任务控制面服务（独立 Spring Boot 应用）
- 适用版本：Java 21、Spring Boot 3.5.4
- Nacos 服务名：`peach-scheduler`（见 `application-dev.yml`）

## 模块定位

`peach-scheduled` 是 Peach Cloud 分布式定时任务的**控制面服务**，负责任务定义治理、执行记录、状态机、Quartz 集群触发、RocketMQ Outbox 分发、Claim 租约、结果回收、人工操作审计与恢复循环。

执行侧 SDK 位于 `peach-component/peach-scheduler`；业务 Handler 在各自微服务中运行，不在本服务内执行。

**本服务提供：**

- 任务 CRUD 与生命周期管理（启用/暂停/禁用/删除/立即执行）
- Execution occurrence 事实、Claim、重试与并发控制
- Handler 白名单注册与心跳
- Quartz JDBC 集群触发（Provider 投影）
- 页面 API 与内部 Claim/注册 API

**本服务不提供：**

- 长耗时业务 Handler 执行
- Exactly-Once 语义
- RUNNING 状态的可靠业务回滚
- 页面动态指定 Java class/SpEL/Shell/SQL
- 独立 RBAC（复用 Peach Auth 的 `scheduler:*` 权限）

可靠性语义：

```text
At-Least-Once Delivery + JDBC Atomic Claim + Idempotent Business Handler
```

## 目录结构

```text
peach-scheduled/
├── peach-scheduled-common/            # 状态、事件、常量、权限编码
├── peach-scheduled-entity/            # DTO、QO、DO、VO
├── peach-scheduled-service/           # Service、DAO、状态机、恢复循环
├── peach-scheduled-rest/              # 页面 API + 内部 API
├── peach-scheduled-openfeign-external/# 业务侧 Feign 适配（Claim/注册）
├── peach-scheduled-launch/            # 启动模块
└── docs/architecture/                 # 架构图源文件
```

## 子模块

| 模块 | 职责 |
| --- | --- |
| `peach-scheduled-common` | `JobState`、`ExecutionState`、事件枚举、`SchedulerConstants`、`SchedulerPermissions` |
| `peach-scheduled-entity` | 持久化 DO 与管理端 DTO/QO/VO |
| `peach-scheduled-service` | 核心业务、`SchedulerReconciler`、`SchedulerTriggerService`、状态机、MyBatis DAO |
| `peach-scheduled-rest` | `/scheduler/**` 页面 API、`/internal/scheduler/**` 内部 API |
| `peach-scheduled-openfeign-external` | 供业务服务依赖的 Feign Client 与 Handler 心跳注册 |
| `peach-scheduled-launch` | 独立服务入口与运行时配置 |

## 核心服务

| 服务 | 说明 |
| --- | --- |
| `SchedulerReconciler` | 将 `PEACH_SCHEDULER_JOB` 同步到 Quartz（`QRTZ_*` 投影） |
| `SchedulerTriggerService` | Quartz/Manual 触发 → 创建 execution → Outbox 分发 |
| `SchedulerExecutionLifecycleService` | Execution 状态迁移、Claim、Complete |
| `SchedulerRetryRecoveryService` | 租约过期恢复、到期重试、DISALLOW 延迟 dispatch |
| `SchedulerJobLifecycleService` | Job 状态机与版本快照 |
| `RocketJobDispatcher` | 默认 `JobDispatcher`，经 `MqOutboxPublisher` 发送执行命令 |
| `SchedulerExecutionResultConsumer` | 消费 `scheduler-execution-result`，驱动 Execution 状态机 |

## 总体架构

图源：`docs/architecture/peach-scheduler-platform.drawio`

```text
peach-cloud-front
       |
       v
peach-scheduled-rest (/scheduler/**)
       |
       v
PEACH_SCHEDULER_JOB  <---- JDBC 事实源
       |
       +--> Spring StateMachine + PEACH_SCHEDULER_STATE_LOG
       |
       +--> SchedulerReconciler --> Quartz (QRTZ_*)
       |
Quartz trigger
       |
       v
PEACH_SCHEDULER_EXECUTION
       |
       +--> RocketJobDispatcher --> MQ_OUTBOX_EVENT --> RocketMQ
       |
       v
业务服务 peach-scheduler-starter
       |
       +--> Claim (/internal/scheduler/executions/{id}/claim)
       +--> @PeachJob Handler
       +--> scheduler-execution-result --> 控制面状态机
```

## 状态机

**Job：**

```text
DRAFT --ENABLE--> ENABLED --PAUSE--> PAUSED --RESUME--> ENABLED
ENABLED/PAUSED --DISABLE--> DISABLED --ENABLE--> ENABLED
DRAFT/DISABLED --DELETE--> DELETED
```

**Execution：**

```text
CREATED --QUEUE--> QUEUED --CLAIM--> RUNNING
RUNNING --SUCCESS--> SUCCEEDED
RUNNING --FAIL--> RETRY_WAIT --RETRY--> QUEUED
RUNNING --TIMEOUT--> TIMED_OUT
RETRY_WAIT --EXHAUST--> DEAD
CREATED/QUEUED/RETRY_WAIT --CANCEL--> CANCELLED
CREATED --SKIP--> SKIPPED
```

持久化策略：每次从 JDBC 当前 state 恢复 Spring StateMachine；更新使用 `WHERE state=? AND version=?` 乐观锁；不维护第二份 JPA 状态。

人工重试仅允许 `RETRY_WAIT`；人工取消仅允许 `CREATED/QUEUED/RETRY_WAIT`；`RUNNING` 不开放通用取消。

## 并发语义

并发由 JDBC 事实保证，不依赖 Quartz 线程生命周期。`SchedulerTriggerService` 对 Job 定义行 `FOR UPDATE`，并以 `QUEUED/RUNNING/RETRY_WAIT` 作为活动 occurrence：

| 策略 | 行为 |
| --- | --- |
| `ALLOW` | 立即写 Outbox 并 `QUEUE` |
| `SKIP_IF_RUNNING` | 创建 occurrence 后转 `SKIPPED`，页面可追溯 |
| `DISALLOW` | 保持 `CREATED`，由 recovery loop 顺序 dispatch |

## REST API

### 页面 API（需登录与 `scheduler:*` 权限）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/scheduler/jobs` | 任务列表 |
| GET | `/scheduler/jobs/{id}` | 任务详情 |
| POST | `/scheduler/jobs` | 创建任务 |
| PUT | `/scheduler/jobs/{id}` | 更新任务定义 |
| POST | `/scheduler/jobs/{id}/enable` | 启用 |
| POST | `/scheduler/jobs/{id}/disable` | 禁用 |
| POST | `/scheduler/jobs/{id}/pause` | 暂停 |
| POST | `/scheduler/jobs/{id}/resume` | 恢复 |
| POST | `/scheduler/jobs/{id}/run` | 立即执行 |
| DELETE | `/scheduler/jobs/{id}` | 删除 |
| GET | `/scheduler/jobs/cron/preview` | Cron 预览 |
| GET | `/scheduler/executions` | 执行历史 |
| GET | `/scheduler/executions/{id}` | 执行详情 |
| POST | `/scheduler/executions/{id}/retry?reason=` | 人工重试 |
| POST | `/scheduler/executions/{id}/cancel?reason=` | 人工取消 |
| GET | `/scheduler/handlers` | 已注册 Handler 列表 |

权限编码见 `SchedulerPermissions`（如 `scheduler:job:run`、`scheduler:execution:retry`）。

### 内部 API（Same-Token，不对公网暴露）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/internal/scheduler/executions/{id}/claim` | 原子抢占 execution 租约 |
| POST | `/internal/scheduler/handlers/register` | 注册/刷新 Handler 白名单 |

## 配置说明

关键配置（见 `peach-scheduled-launch/src/main/resources/application-dev.yml`）：

```yaml
spring:
  application:
    name: peach-scheduler
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
  datasource:
    url: jdbc:mysql://...

mybatis:
  mapper-locations: classpath*:com/peach/scheduler/dao/*.xml

peach:
  scheduler:
    enabled: true
    provider: quartz
    rocket:
      require-jdbc: true
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.scheduler.provider` | `quartz` | 当前启用的 `SchedulingProvider` ID |
| `peach.scheduler.rocket.require-jdbc` | 生产建议 `true` | 禁止内存 Outbox/幂等 |
| `peach.scheduler.service.reconcile-delay-ms` | `5000` | Job 定义同步到 Quartz 的间隔 |
| `peach.scheduler.service.recovery-delay-ms` | `5000` | 租约恢复/重试/延迟 dispatch 扫描间隔 |
| `peach.scheduler.service.handler-offline-scan-ms` | `60000` | Handler 离线扫描间隔 |

RocketMQ Topic：

| Topic | 说明 |
| --- | --- |
| `scheduler-execute-{applicationName}` | 控制面向业务服务下发执行命令 |
| `scheduler-execution-result` | 业务服务回传执行结果 |

## SQL 与 Mapper

脚本位于仓库根目录 `sql/`，说明见 `sql/README.md`。

| 表/脚本 | 说明 |
| --- | --- |
| `PEACH_SCHEDULER_JOB` | 任务定义事实源 |
| `PEACH_SCHEDULER_JOB_VERSION` | 配置版本快照 |
| `PEACH_SCHEDULER_EXECUTION` | 逻辑 occurrence |
| `PEACH_SCHEDULER_EXECUTION_ATTEMPT` | 重试尝试 |
| `PEACH_SCHEDULER_STATE_LOG` | 状态迁移审计 |
| `PEACH_SCHEDULER_HANDLER` | Handler 白名单 |
| `PEACH_SCHEDULER_OPERATION_LOG` | 人工操作审计 |
| `QRTZ_*` / `QRTZ_MYSQL.sql` | Quartz 集群投影 |
| `MQ_OUTBOX_EVENT` / `MQ_CONSUME_RECORD` | Rocket 持久化 |

MyBatis XML 路径：

```text
peach-scheduled-service/src/main/resources/com/peach/scheduler/dao/
```

## 业务服务接入要点

1. 业务服务引入 `peach-scheduler-starter` + `peach-scheduled-openfeign-external`。
2. 实现 `@PeachJob` Handler 并声明 `scheduler-execute-{app}` Consumer。
3. 在控制面创建任务时，`applicationName` 与 `handlerName` 必须匹配已注册白名单。
4. Feign 目标服务名为 `peach-scheduler`，需传播 Peach Same-Token。

## 安全边界

- 禁止页面提交可执行表达式（class/SpEL/Shell/SQL 等）。
- 任务参数 JSON 禁止 password/token/secret/accessKey 等字段。
- 日志只记录稳定业务标识，不记录完整 DTO 或凭据。
- Claim/注册接口校验 Same-Token，不依赖用户登录态。
- 写操作、Claim、Complete 均使用乐观锁（state + version）。

## 生产前置

1. 执行 `sql/` 下 `PEACH_SCHEDULER_*`、`QRTZ_MYSQL.sql`、`MQ_OUTBOX_EVENT.sql`、`MQ_CONSUME_RECORD.sql`（或 `ALL_TABLE_CREATE.sql`）。
2. 使用 MySQL JDBC JobStore，禁止 RAMJobStore；`initialize-schema=never`。
3. 提前创建 RocketMQ Topic：`scheduler-execution-result` 与各业务 `scheduler-execute-{app}`。
4. `/internal/scheduler/**` 仅内网可达，并保持 Same-Token 校验。
5. 为 execution、state log、attempt、operation log、outbox、consume record 配置 retention 清理。

## 构建与验证

```bash
mvn -f peach-scheduled/pom.xml -Dmaven.test.skip=true package -Pdevelopment
node scripts/check-utf8.mjs
git diff --check -- peach-scheduled sql
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 任务未触发 | Job 是否 `ENABLED`、`SYNC_STATUS` 是否 `SYNCED` | 查 `SchedulerReconciler` 日志与 `LAST_SYNC_ERROR` |
| Quartz 有触发但无 execution | `SchedulerTriggerService`、Job 状态 | 确认 Job 非 `PAUSED/DISABLED` |
| 业务未收到 MQ | Outbox 状态、Topic 名称 | 查 `MQ_OUTBOX_EVENT`，确认 topic 为 `scheduler-execute-{app}` |
| Claim 失败 | execution 是否 `QUEUED`、version 是否匹配 | 查 `PEACH_SCHEDULER_EXECUTION` 与 Same-Token |
| 重复执行 | 仅 Claim 不足 | 要求业务 Handler 基于 `executionId` 幂等 |
| Handler 不可选 | `PEACH_SCHEDULER_HANDLER` 是否 ONLINE | 确认业务服务 Feign 注册与心跳 |
| 启动 fail-fast | `require-jdbc=true` 但缺表或内存 Store | 执行 SQL 并检查 JDBC Bean |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
