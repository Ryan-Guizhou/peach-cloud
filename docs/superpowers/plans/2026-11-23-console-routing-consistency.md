# Console routing consistency implementation plan

> **Database plan superseded — 2026-11-24：** 项目未上线，用户已改为全新空 schema 的完整 V1 初始化（schema、seed、编码及权限）。文中的 V12/V13/V14 增量升级与“保留历史编码”描述是早期决定和排障记录，不再是当前迁移操作指引。最终四类业务编码采用 M/F/RT/RL 四位；UI、路由解析与普通用户权限边界保持本计划已验证行为。旧开发库须新建 schema，或由使用者明确备份后重建，不自动 clean/repair/兼容。

**Goal:** 统一浅色控制台与认证页面，恢复全部已实现页面的管理员导航，新增路由接入统一编码。

**Architecture:** 后端权限快照仍为事实源；前端通过本地组件白名单匹配路由与菜单。复用 AdminLayout 和 Ant Design Vue，新增 Flyway 迁移升级规则与系统导航。

**Tech Stack:** Vue 3, TypeScript, Ant Design Vue, Java 21, Spring Boot, MySQL, Flyway.

**Spec:** 本次会话中用户批准的 Implementation Contract。

## Constraints

- 历史编码、主键及 URL 保留；新增 ROUTER 类型使用统一发号。
- 管理员通过真实后端授权获得完整已实现功能，普通用户依旧按权限展示。
- 用户已授权开始；不重复请求实现许可。不提交或推送远程变更。
- 保留已有工作台修复与无关的 mq/ 内容。

## Review focus

未知组件、重复路由名、空权限、菜单指向错误组件、旧库规则升级、既有自定义数据、窄屏和认证表单状态。

## Tasks

- [x] UI: 统一全局浅色 token、AdminLayout、工作台、个人中心和认证布局；移除虚构工作台监控信息；构建及浏览器验证。
- [x] Routing: 扩展动态路由测试，验证登录日志不会映射为工作台、菜单只指向有效路由、普通用户空授权无业务兜底；修复解析并执行 npm 测试。
- [x] Backend/data: 路由新增统一发号、更新编码保持不可变；兼容旧编码规则的迁移及账本；补齐管理员系统导航绑定而不放开普通用户权限；执行受影响 Maven 测试。
- [x] Integration: 核对页面/菜单/组件覆盖，更新 DESIGN.md 与相关文档，执行 node scripts/verify-changes.mjs、前端测试与构建，审查最终差异与浏览器结果。

## Execution ledger

- 2026-11-23: 用户授权开始；使用现有共享工作区以保留前序未提交修复。两项独立实现并行，最终集成验证由主 Agent 完成。
- 后续缺失菜单/403 修复：只读业务库确认已执行 V13，但缺失多项资源定义，`POST:/api/auth/router/pageList` 的定义和授权均为零。新增 V14 补资源与管理员授权，保持已执行 V13 不变；浏览器回归扩展文件记录、存储实例、对象浏览入口。
