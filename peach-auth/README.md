# peach-auth

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-auth`  
类型：认证与权限业务域聚合模块

## 模块定位


`peach-auth` 承载用户、角色、菜单、资源、路由权限、登录态和用户操作日志相关能力。它提供实体模型、领域服务、REST 接口、OpenFeign 外部接口、操作日志注解能力和可启动服务。

本模块解决：

- 认证域核心数据模型和服务分层。
- 登录、用户、角色、菜单、资源、路由等 REST 入口。
- 面向其他服务的角色、路由等外部查询接口。
- `@UserOperLog` 操作日志采集入口。

本模块不解决：

- 网关过滤、统一鉴权拦截和网关路由转发，相关逻辑位于 `peach-gateway` 与 `peach-satoken`。
- Sa-Token、Redis、数据库等基础设施部署。
- 生产密码策略、限流、风控、审计归档等完整安全治理。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-auth-common` | 认证域公共常量、工具和共享对象 |
| `peach-auth-entity` | DO、DTO、QO、VO 等认证域模型 |
| `peach-auth-service` | 用户、角色、菜单、资源、登录、权限等领域服务 |
| `peach-auth-rest` | 内部 REST 接口和外部 REST 适配入口 |
| `peach-auth-external/peach-auth-openfeign-external` | 面向其他模块的 OpenFeign 客户端 |
| `peach-auth-external/peach-auth-log-external` | 用户操作日志注解、枚举和外部能力 |
| `peach-auth-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-auth-launch/src/main/java/com/peach/auth/launch/PeachAuthServiceApplication.java` |
| 配置文件 | `peach-auth-launch/src/main/resources/application-dev.yml` |
| REST 包 | `peach-auth-rest/src/main/java/com/peach/auth/rest` |
| 服务接口 | `peach-auth-service/src/main/java/com/peach/auth/service` |
| 实体模型 | `peach-auth-entity/src/main/java/com/peach/auth/entity` |
| OpenFeign | `peach-auth-external/peach-auth-openfeign-external/src/main/java/com/peach/auth/openfeign` |

## REST 能力

当前 REST 控制器覆盖以下入口：

| 控制器 | 路径前缀 | 说明 |
| --- | --- | --- |
| `LoginController` | `/auth` | 登录和认证相关入口 |
| `UserController` | `/auth` | 用户相关接口 |
| `RoleController` | `/auth/role` | 角色管理 |
| `MenuController` | `/auth/menu` | 菜单管理 |
| `ResourceController` | `/auth/resource` | 资源管理 |
| `RouterController` | `/auth/router` | 路由权限 |
| `FileController` | `/auth/file` | 认证域文件相关入口 |
| `RoleExternalController` | `/auth/external` | 外部角色查询接口 |
| `RouterExternalController` | `/auth/external` | 外部路由查询接口 |

## 运行机制

1. `peach-auth-launch` 启动服务并加载当前 profile 的配置。
2. REST 层接收登录、用户、角色、菜单、资源、路由等请求。
3. Service 层组织认证域业务逻辑，并通过 DAO 访问数据库。
4. 外部模块通过 `peach-auth-openfeign-external` 或网关路由访问认证能力。
5. 使用 `@UserOperLog` 的业务动作会进入操作日志链路，具体持久化依赖服务实现和数据库配置。

## 配置说明

- 启动配置位于 `peach-auth-launch/src/main/resources/application-*.yml`。
- 本地、Docker、生产环境分别使用 `dev`、`docker`、`prod` profile。
- 数据库、Redis、Nacos、Sa-Token 参数需要结合运行环境配置，不能把生产密钥写入仓库。
- 权限缓存和会话持久化依赖外部 Redis / Sa-Token 配置是否正确。

## 边界与限制

- 认证域提供权限数据和登录接口，但网关侧鉴权是否放行由网关和 Sa-Token 配置决定。
- 外部接口面向服务间调用，不应直接暴露为无鉴权公网接口。
- 操作日志只能记录进入注解切面的调用，无法覆盖绕过 Spring Bean 或未标注注解的行为。
- 登录失败重试、验证码、密码复杂度、账号锁定等生产安全策略需要按业务补齐。

## 构建与验证

```bash
mvn -f "peach-auth/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-auth/peach-auth-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-auth/peach-auth-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 登录接口不可用 | `peach-auth-launch` 是否启动；网关路由是否指向认证服务 | 先直连认证服务端口验证，再检查 `peach-gateway` 路由 |
| 权限数据为空 | 数据库脚本是否执行；用户、角色、资源关系是否存在 | 检查 `sql/` 初始化脚本和认证域表数据 |
| OpenFeign 调用失败 | 服务名、Nacos 注册、Feign 配置是否正确 | 确认认证服务已注册，调用方引入 `peach-auth-openfeign-external` |
| 操作日志未记录 | 是否使用 `@UserOperLog`；调用是否经过 Spring 代理 | 检查注解位置、AOP 生效条件和日志服务实现 |
| 会话或权限缓存异常 | Redis、Sa-Token、序列化配置是否一致 | 检查 Redis 连接和 `peach-satoken` 相关配置 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。


## 调度联调案例

在 IDEA 中使用现有应用入口和正常的 `dev`（或 `docker`）环境启动。本地 application YAML 保留启动与 Nacos 导入配置；调度、受管 `scheduler` 虚拟线程组、RocketMQ、Outbox 和任务默认定义由 Nacos 管理。服务自动登记 `authSchedulerDemo`、发送心跳并消费执行命令，经受管执行器 Claim 后执行。案例仅返回成功，不修改业务数据。可通过 `PEACH_SCHEDULER_DEMO_ENABLED=false` 关闭测试 Handler 和消费者。

服务启动后自动登记任务，可在任务管理查看定义和初始状态。业务服务与控制面需要共用可访问的 Nacos、Redis、MySQL Outbox 和 RocketMQ 环境。实际执行需在线验证。


## 任务自动登记

服务通过 `@PeachJob` 提供稳定的 `jobCode`、`jobName`，在 Nacos 的 `peach.scheduler.executor.jobs[handlerName]` 提供完整默认任务定义，包括周期、时区、误触发与并发策略、超时、重试、参数和初始状态。控制面首次登记时校验定义并创建任务；元数据缺失时只登记 Handler 和刷新心跳，不创建占位草稿。后续心跳不覆盖管理端已维护的周期、参数、启停或删除状态。同一 Handler 仍可关联其他任务。初始状态由集中配置维护。

## 租户管理

租户列表通过 `/tenant` 页面进入，接口为 `/auth/tenant` 下的分页、详情、新增、修改、状态变更与逻辑删除。沿用现有管理员及菜单/API/按钮资源授权，不引入新的平台角色。

租户状态为 `PENDING → ACTIVE ↔ SUSPENDED → CLOSED`，待激活租户也可直接注销。仅待激活或已注销租户允许逻辑删除，当前登录租户不可停用、注销或删除。更新、状态变更和删除均检查版本号；创建机构和账号不在此功能范围内。

已有数据库先备份并执行[租户增量迁移](../sql/migrations/20261121_tenant_management.sql)。迁移保留租户业务 ID 和用户/机构关联，将旧 `ENABLE` 状态转为 `ACTIVE`，保留旧列便于回退；MySQL DDL 不支持事务回滚。停用后阻止新的登录和上下文切换，并在事务提交后撤销该租户的既有会话。管理员重新登录后加载新增菜单与资源授权。

## 功能定义维护

`POST /auth/function/list` 返回当前登录租户的完整功能列表，按排序号、功能编码排序，供前端构建功能树。`/auth/resource/pageList` 支持功能编码、资源类型和编码/名称关键词 `keyword` 筛选。新增功能和资源时可省略 ID，由服务端生成。

功能编码创建后不可修改；上级必须属于同一租户且不能形成环。删除功能前检查下级、资源、菜单、路由及授权引用；已有授权的资源不可删除或修改编码/类型。功能与资源的维护使用事务和功能行锁；其他模块直接写入关联表时仍须遵守引用约束。

已有环境执行 [迁移 SQL](../sql/migrations/20261122_function_definition.sql) 并重新登录。`FunctionDefinitionTest` 覆盖服务规则；显式设置 `PEACH_FUNCTION_DB_SMOKE=true` 后，`FunctionDefinitionDatabaseTest` 从 Nacos 在内存读取数据源配置，在 MySQL 验证流程并回滚所有测试写入。

## 登录审计与功能分类

提供独立事务登录审计、租户隔离的只读查询和默认90天保留期清理。功能树按功能列表根、系统设置、日志审计、文件与存储、任务调度组织。升级步骤、权限与采集边界见 [登录审计](docs/login-audit.md)。

## 角色、路由与操作日志

角色工作区支持功能与资源原子授权、成员维护、默认菜单及校准预览；路由支持组合筛选。操作日志补全租户机构映射并保留失败批次重试。迁移、生效与可靠性边界见 [管理工作区说明](docs/admin-workspace.md)。
