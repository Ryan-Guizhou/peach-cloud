# Peach Database

[English](README.en-US.md)

项目应用专用的共享 MySQL 迁移 JAR。七个 JDBC 应用启动时由 Spring Boot 执行同一套 Flyway 迁移，使用同库 `PEACH_FLYWAY_HISTORY` 管理版本。Flyway 版本由 Spring Boot 依赖管理统一确定；它不是可复用业务 Starter。

## 启动与升级

构建并部署依赖此模块的应用，使用已有数据源配置启动。启动迁移失败会阻止应用就绪。启动前备份数据库；完成升级后重新登录，刷新菜单和权限快照。显式设置 `spring.flyway.enabled=false` 可关闭 Boot 启动迁移，此时部署方必须自行完成兼容迁移。

```mermaid
flowchart LR
    A[应用启动] --> B[获取 MySQL GET_LOCK]
    B --> C[检查既有库签名]
    C --> D[Flyway 校验并迁移]
    D --> E[释放锁并继续启动]
```

命名锁覆盖首次接管检查和完整迁移，等待上限 120 秒。空库直接迁移；没有历史表的非空库必须包含 `PEACH_USER.USER_ID`、`PEACH_TENANT.TENANT_ID`、`PEACH_FUNCTION.FUNC_CODE`，再以版本 0 接管。这只是有限签名检查，不是完整数据库差异检查，不能证明任意历史库都兼容。已有历史表的数据库由 Flyway 校验已执行版本和校验和。

V1 Java 迁移执行打包的表结构，V5 Java 迁移执行系统种子，两者均提供源文件校验和；其余版本迁移维护 MQ、租户、任务注册、权限、登录审计和角色工作区等历史增量。当前源码及测试是版本清单的事实源。

## SQL 维护边界

首次发布前运行以下命令整理并检查基线与部署镜像：

```bash
node scripts/sync-database-sql.mjs
node scripts/sync-database-sql.mjs --check
node --test scripts/sync-database-sql.test.mjs
```

命令在仓库根执行。生成器按文件名排序合并 `sql/` 下含 CREATE TABLE 的单表脚本，排除 `INIT.sql`、`ALL_TABLE_CREATE.sql`；同步 `sql/ALL_TABLE_CREATE.sql`、`deploy-pipline/init/mysql/schema/ALL_TABLE_CREATE.sql`，并将 `sql/INIT.sql` 复制到 `deploy-pipline/init/mysql/data/INIT.sql`。`--check` 发现漂移时失败且不写文件。保留文件名是因为两套部署入口仍引用这些路径。

**首次 Flyway 发布后冻结 `sql/ALL_TABLE_CREATE.sql`、`sql/INIT.sql` 及已发布迁移。** 未来变更只增加版本迁移，不修改基线或通过重新生成基线改变已发布校验和。生成器用于当前首次发布前整理漂移；发布后 `--check` 用于检查镜像一致性。`sql/migrations/` 保留历史手工脚本作为归档，正常启动升级不再要求逐个手动执行，也不再使用临时 Python 迁移工具。

## 故障处理与清理范围

MySQL DDL 可能隐式提交，启动失败不能保证整体回滚。失败后保留备份与迁移历史，定位并修复原因，检查实际表结构和数据，再按受控流程决定恢复或修复历史。禁止盲目重试修改过的历史迁移。本模块启用迁移校验、禁用 clean，不提供自动 repair 或自动清库。

删除了根目录临时 RocketMQ Compose 与 broker 配置，正式入口仍为 `deploy-pipline/compose/middleware/docker-compose.yml`；生成的前端审计报告已移除并忽略，避免提交机器路径和过期结论。浏览器回归脚本、测试及日志公共辅助类保留，避免丢失回归证据或破坏调用兼容性。操作日志仍使用进程内存队列，进程崩溃或持续数据库故障的丢失风险未改变；本次迁移不增加持久化日志补偿。

进一步阅读：[登录审计](../peach-auth/docs/login-audit.md)、[管理工作区](../peach-auth/docs/admin-workspace.md)。

接管存量库时 V5 不重放初始管理员、成员及全量授权种子，避免恢复已经撤销的配置；增量迁移仍按版本升级系统定义。Hikari 连接池的 `maximumPoolSize` 至少为 3，迁移启动会提前拒绝过小的配置，避免锁连接耗尽连接池。
