# 角色工作区、路由管理与操作日志

[English](admin-workspace.en-US.md)

## 角色与路由

角色页按角色列表、功能树、资源列表三列展示。角色关键词按编码或名称查询；路由支持模块编码、路由编码、URL 的组合包含查询。查询范围由服务端绑定当前租户；角色同时限定机构，路由限定当前租户。路由列表与编辑器隐藏是否鉴权、是否缓存，未提交的历史标记保持原值。路由编辑实际执行更新，编码在当前租户内唯一，组件必须来自前端已有页面。

功能树使用独立勾选，筛选保留祖先路径，隐藏节点与资源的授权不因筛选而丢失。授权按钮一次保存完整功能、资源草稿。资源必须属于已选功能；已有失效授权保留展示，供显式删除或校准。校准先返回失效、禁用及不属于所选功能的授权差异，确认仅修改草稿，点击授权才写库，不补发新授权。

成员维护仅替换本角色关联，保留用户其他角色。默认菜单只能来自该角色已保存授权中的可用菜单；空值恢复工作台。仅单角色登录采用默认菜单，多角色仍进入工作台，显式登录回跳及路由权限校验继续生效。默认菜单调整在下次登录生效。

新工作区写入口使用 READ_COMMITTED 事务及角色行锁；授权保存还锁定所选功能。授权与成员分别携带修订号，拒绝旧快照覆盖。功能、资源在同一事务提交；用户权限缓存只在事务提交后刷新。角色编码创建后不可修改，关联未解除时拒绝删除。

## 工作区接口

所有路径前缀为 `/auth/role`。读接口使用 `roleId` 查询参数；写接口接收 `RoleWorkspaceDTO`，必填 `roleId`、`revision`，相关集合必须完整提交。

| 方法与路径 | 数据 | 业务权限 |
| --- | --- | --- |
| GET `/workspace` | 角色、定义、授权、候选菜单、修订号 | `role:view` |
| GET `/members` | 可选用户、本角色成员、成员修订号 | `authorization:update` |
| POST `/saveGrants` | `funcCodes`、`resourceKeys` | `authorization:update` |
| POST `/calibrate` | 同上，只返回移除差异 | `authorization:update` |
| POST `/saveMembers` | `userCodes`、成员修订号 | `authorization:update` |
| POST `/saveDefault` | `skipUrl`、授权修订号 | `role:update` |

资源键是功能编码、类型、资源编码依次拼接的长度前缀编码；前后端分别由 `RoleWorkspaceService.key` 与 `views/system/role/state.ts` 实现。新增 API 资源仍受网关 API 授权限制，业务层再次检查当前用户的对应按钮权限。

## 迁移与生效

存量数据库由启动迁移升级；`sql/migrations/20261122_role_route_workspace.sql` 保留为历史归档，新库初始化脚本已同步。迁移将 `PEACH_ROLE.SKIP_URL` 扩到 100 字符，注册 6 个 API，并从现有对应按钮授权派生 API 授权，不新增按钮权限。

部署前备份数据库，随后由应用启动时的共享 Flyway 迁移执行升级，操作边界见 [数据库迁移](../../peach-database/README.md)。临时 Python 工具已移除；MySQL DDL 可能隐式提交，失败后核对实际结构和迁移历史，不自动 repair 或清库。

构建并重启 auth 服务后重新登录，加载新接口、操作日志采集器和权限快照。IDE 中已运行的旧进程不会因源码修改自动具备新 Bean。

## 操作日志

`com.peach.log` 下的切面在正常返回、业务失败和异常退出时采集带 `@UserOperLog` 的操作，不改变业务返回值或原始异常。租户、机构、操作者在请求线程从可信安全上下文获取；缺少可信范围时不写入无归属日志。补全 `USER_OPER_LOG.TENANT_ID/ORG_ID` 的模型和 SQL 映射，解决现有表必填字段导致的插入失败。

默认每 5000ms 消费最多 200 条，间隔配置为 `peach.operation-log.flush-interval-millis`。失败批次保留并在下次调度重试，沿用相同 ID；重复插入按主键幂等。日志单独批量提交，业务回滚时失败操作仍可记录。查询限定当前租户、机构。

只采集静态操作名、允许的对象标识、结果、耗时、方法及请求路径。忽略历史注解中拼接完整 DTO 的 SpEL，不保存完整请求、响应或原始异常消息。允许的目标字段为 roleId、roleCode、routerId、routerCode、userId、funcId、funcCode、resourceId，值限定字母、数字、下划线和横线。IP 为服务直接对端地址，不信任转发头。

队列与重试批次仍在进程内存中；进程崩溃可能丢失未提交日志，持续写库失败会阻塞当前批次。此实现不提供持久化 Outbox 保证。日志仅覆盖已注解操作；系统日志及 Loki 接入不在本次范围。

## 验证入口

`OperationLogRegressionTest` 验证异常采集和批次重试；`RoleWorkspaceSecurityTest` 验证权限、作用域、修订号相关边界与登录默认菜单；设置 `PEACH_ADMIN_DB_SMOKE=true` 后，`AdminWorkspaceDatabaseTest` 使用实际 MySQL 验证组合查询、更新、授权、成员隔离及切面到队列到数据库的成功/失败日志和幂等插入，清理独立测试数据。

前端构建运行 `npm.cmd --prefix peach-cloud-front run build`。`scripts/admin-workspace-smoke.py` 在构建后的页面上通过隔离 API fixtures 验证筛选保留授权、校准、成员、默认菜单、路由编辑及窄屏布局；不等同于当前运行服务的端到端验收。
