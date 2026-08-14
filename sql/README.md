# SQL 脚本维护说明

## 说明

`sql` 目录采用“单表脚本 + 总表脚本 + 初始化脚本”的组织方式：

- `PEACH_*.sql`：单表建表脚本，每个文件只维护一张表的 `CREATE TABLE` 定义。
- `USER_OPER_LOG.sql`：独立日志表建表脚本。
- `PEACH_TENANT.sql`：租户主数据建表脚本。
- `PEACH_CODE_RULE.sql`：机构维度业务编码规则建表脚本。
- `PEACH_USER_ORG.sql`：用户与机构关系表建表脚本。
- `QRTZ_MYSQL.sql`：Quartz JDBC 集群多表脚本（调度 Provider 投影）。
- `MQ_OUTBOX_EVENT.sql`、`MQ_CONSUME_RECORD.sql`：RocketMQ 持久化脚本（调度与消息组件共用）。
- `ALL_TABLE_CREATE.sql`：由各个单表脚本聚合生成的总建表脚本。
- `INIT.sql`：初始化脚本，用于清库、初始化基础数据或应用种子数据，不作为单表定义源文件。

## 脚本用途索引

### 平台基础表

| 脚本 | 用途 |
| --- | --- |
| `PEACH_APPLICATION.sql` | 应用注册 |
| `PEACH_TENANT.sql` | 租户主数据 |
| `PEACH_ORGANIZATION.sql` | 组织机构 |
| `PEACH_USER.sql` | 用户账号 |
| `PEACH_USER_ORG.sql` | 用户与机构关系 |
| `PEACH_ROLE.sql` | 角色 |
| `PEACH_FUNCTION.sql` | 功能菜单定义 |
| `PEACH_MENU.sql` | 菜单树 |
| `PEACH_ROUTER.sql` | 前端路由 |
| `PEACH_RESOURCE.sql` | 按钮/API 权限资源 |
| `PEACH_AUTH_FUNCTION.sql` | 角色功能授权 |
| `PEACH_AUTH_RESOURCE.sql` | 角色资源授权 |
| `PEACH_AUTH_PARTY.sql` | 授权主体 |
| `PEACH_AUTH_LOG.sql` | 认证日志 |
| `PEACH_DICT_TYPE.sql` / `PEACH_DICT_ITEM.sql` | 数据字典 |
| `PEACH_VALUE_SET.sql` / `PEACH_VALUE_SET_ITEM.sql` | 值集 |
| `PEACH_LANGUAGE.sql` | 多语言 |
| `PEACH_CODE_RULE.sql` | 业务编码规则 |
| `PEACH_NOTICE.sql` / `PEACH_NOTICE_READ_RECORD.sql` | 通知公告 |
| `PEACH_SITE_MESSAGE.sql` / `PEACH_MULTI_MESSAGE.sql` | 站内信 |
| `PEACH_FILE_OBJECT.sql` / `PEACH_FILE_RECORD.sql` / `PEACH_FILE_UPLOAD_SESSION.sql` | 文件服务 |
| `PEACH_STORAGE_INSTANCE.sql` | 云存储实例 |
| `PEACH_IP_WHITELIST.sql` | IP 白名单 |
| `PEACH_USER_AVATAR_HISTORY.sql` | 用户头像历史 |
| `USER_OPER_LOG.sql` | 用户操作审计日志 |

### 调度模块（`peach-scheduled` + `peach-scheduler`）

| 脚本 | 用途 | 主要使用方 |
| --- | --- | --- |
| `PEACH_SCHEDULER_JOB.sql` | 任务定义事实源 | `peach-scheduled` 控制面 |
| `PEACH_SCHEDULER_JOB_VERSION.sql` | 任务定义版本快照 | `peach-scheduled` |
| `PEACH_SCHEDULER_EXECUTION.sql` | 逻辑执行 occurrence、Claim 租约 | `peach-scheduled` |
| `PEACH_SCHEDULER_EXECUTION_ATTEMPT.sql` | 每次重试尝试历史 | `peach-scheduled` |
| `PEACH_SCHEDULER_HANDLER.sql` | 业务 Handler 注册白名单 | `peach-scheduled` |
| `PEACH_SCHEDULER_STATE_LOG.sql` | Job/Execution 状态迁移审计 | `peach-scheduled` |
| `PEACH_SCHEDULER_OPERATION_LOG.sql` | 人工 run/retry/cancel 操作审计 | `peach-scheduled` |
| `QRTZ_MYSQL.sql` | Quartz JDBC 集群表（Provider 运行时投影，非事实源） | `peach-scheduled` |
| `MQ_OUTBOX_EVENT.sql` | 执行命令/结果可靠发送 Outbox | `peach-scheduled`、`peach-scheduler-transport-rocket` |
| `MQ_CONSUME_RECORD.sql` | MQ 消费幂等，防重复执行业务 Handler | `peach-scheduler-transport-rocket` |

调度模块生产前置：

1. 先执行 `PEACH_SCHEDULER_*`，再执行 `QRTZ_MYSQL.sql` 与 Rocket 两张表。
2. `spring.quartz.jdbc.initialize-schema=never`，禁止应用自动建 Quartz 表。
3. `peach.scheduler.rocket.require-jdbc=true` 时必须存在 `MQ_OUTBOX_EVENT` 与 `MQ_CONSUME_RECORD`。
4. 调度中心菜单与 `scheduler:*` 权限由平台权限模块维护，不在 SQL 目录提供种子脚本。

## 维护原则

1. 修改表结构时，优先修改对应的单表脚本。
2. 修改完单表脚本后，重新生成 `ALL_TABLE_CREATE.sql`。
3. 不建议手工编辑 `ALL_TABLE_CREATE.sql`，它应始终与单表脚本保持一致。
4. `INIT.sql` 只维护初始化流程和初始化数据，不在这里承载单表结构的长期维护。

## 推荐流程

1. 找到对应的单表脚本，例如 `PEACH_USER.sql`。
2. 修改字段、索引、注释或约束。
3. 重新生成 `ALL_TABLE_CREATE.sql`。
4. 检查 `git diff --check`，确认没有行尾空格和格式问题。

## 当前聚合范围

`ALL_TABLE_CREATE.sql` 当前聚合以下脚本：

- `PEACH_APPLICATION.sql`
- `PEACH_AUTH_FUNCTION.sql`
- `PEACH_AUTH_LOG.sql`
- `PEACH_AUTH_PARTY.sql`
- `PEACH_AUTH_RESOURCE.sql`
- `PEACH_CODE_RULE.sql`
- `PEACH_DICT_ITEM.sql`
- `PEACH_DICT_TYPE.sql`
- `PEACH_FILE_OBJECT.sql`
- `PEACH_FILE_RECORD.sql`
- `PEACH_FILE_UPLOAD_SESSION.sql`
- `PEACH_FUNCTION.sql`
- `PEACH_IP_WHITELIST.sql`
- `PEACH_LANGUAGE.sql`
- `PEACH_MENU.sql`
- `PEACH_MULTI_MESSAGE.sql`
- `PEACH_NOTICE.sql`
- `PEACH_NOTICE_READ_RECORD.sql`
- `PEACH_ORGANIZATION.sql`
- `PEACH_RESOURCE.sql`
- `PEACH_ROLE.sql`
- `PEACH_ROUTER.sql`
- `PEACH_SCHEDULER_EXECUTION.sql`
- `PEACH_SCHEDULER_EXECUTION_ATTEMPT.sql`
- `PEACH_SCHEDULER_HANDLER.sql`
- `PEACH_SCHEDULER_JOB.sql`
- `PEACH_SCHEDULER_JOB_VERSION.sql`
- `PEACH_SCHEDULER_OPERATION_LOG.sql`
- `PEACH_SCHEDULER_STATE_LOG.sql`
- `PEACH_SITE_MESSAGE.sql`
- `PEACH_STORAGE_INSTANCE.sql`
- `PEACH_TENANT.sql`
- `PEACH_USER.sql`
- `PEACH_USER_AVATAR_HISTORY.sql`
- `PEACH_USER_ORG.sql`
- `PEACH_VALUE_SET.sql`
- `PEACH_VALUE_SET_ITEM.sql`
- `MQ_CONSUME_RECORD.sql`
- `MQ_OUTBOX_EVENT.sql`
- `QRTZ_MYSQL.sql`
- `USER_OPER_LOG.sql`

## 重新生成

可以使用仓库根目录下的 PowerShell 命令重新生成总表脚本。生成后请确认：

```powershell
$order = @(
  'PEACH_APPLICATION.sql',
  'PEACH_AUTH_FUNCTION.sql',
  'PEACH_AUTH_LOG.sql',
  'PEACH_AUTH_PARTY.sql',
  'PEACH_AUTH_RESOURCE.sql',
  'PEACH_CODE_RULE.sql',
  'PEACH_DICT_ITEM.sql',
  'PEACH_DICT_TYPE.sql',
  'PEACH_FILE_OBJECT.sql',
  'PEACH_FILE_RECORD.sql',
  'PEACH_FILE_UPLOAD_SESSION.sql',
  'PEACH_FUNCTION.sql',
  'PEACH_IP_WHITELIST.sql',
  'PEACH_LANGUAGE.sql',
  'PEACH_MENU.sql',
  'PEACH_MULTI_MESSAGE.sql',
  'PEACH_NOTICE.sql',
  'PEACH_NOTICE_READ_RECORD.sql',
  'PEACH_ORGANIZATION.sql',
  'PEACH_RESOURCE.sql',
  'PEACH_ROLE.sql',
  'PEACH_ROUTER.sql',
  'PEACH_SCHEDULER_EXECUTION.sql',
  'PEACH_SCHEDULER_EXECUTION_ATTEMPT.sql',
  'PEACH_SCHEDULER_HANDLER.sql',
  'PEACH_SCHEDULER_JOB.sql',
  'PEACH_SCHEDULER_JOB_VERSION.sql',
  'PEACH_SCHEDULER_OPERATION_LOG.sql',
  'PEACH_SCHEDULER_STATE_LOG.sql',
  'PEACH_SITE_MESSAGE.sql',
  'PEACH_STORAGE_INSTANCE.sql',
  'PEACH_TENANT.sql',
  'PEACH_USER.sql',
  'PEACH_USER_AVATAR_HISTORY.sql',
  'PEACH_USER_ORG.sql',
  'PEACH_VALUE_SET.sql',
  'PEACH_VALUE_SET_ITEM.sql',
  'MQ_CONSUME_RECORD.sql',
  'MQ_OUTBOX_EVENT.sql',
  'QRTZ_MYSQL.sql',
  'USER_OPER_LOG.sql'
)

$builder = New-Object System.Collections.Generic.List[string]
$builder.Add('-- ALL_TABLE_CREATE.sql')
$builder.Add('-- Generated from individual table scripts under /sql')
$builder.Add('-- Do not edit this file directly; edit the source table script and regenerate.')
$builder.Add('')

foreach ($name in $order) {
  $path = Join-Path .\sql $name
  if (Test-Path $path) {
    $builder.Add('-- ' + $name)
    $builder.Add((Get-Content -LiteralPath $path -Raw -Encoding UTF8).Trim())
    $builder.Add('')
  }
}

[System.IO.File]::WriteAllText(
  (Resolve-Path .\sql\ALL_TABLE_CREATE.sql),
  (($builder -join "`r`n").TrimEnd() + "`r`n"),
  (New-Object System.Text.UTF8Encoding($false))
)
```

生成后请确认：

- 文件顺序保持稳定
- 每个表脚本只出现一次
- `ALL_TABLE_CREATE.sql` 末尾没有多余空行
- 文件编码为 UTF-8

## 备注

如果后续新增了新的表脚本，只需要：

1. 新增或修改对应的单表脚本。
2. 更新 `ALL_TABLE_CREATE.sql` 的聚合列表。
3. 同步检查 `INIT.sql` 是否需要补充初始化语句。
