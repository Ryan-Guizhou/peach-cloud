# 调度管理界面

## Goal and scope

实现任务管理、执行记录、全部 Handler 三个页面；契约来源为 `peach-scheduled` 的管理 Controller、Save DTO、Query、VO 和状态机。用户已确认继续实现，其他模块行为不变。暂不增加调度权限码过滤，仍要求登录和真实操作人审计。

## Visual direction and ownership

沿用存储实例页面的控制台布局、白色卡片、表格、查询栏和 Ant Design Vue 弹窗。排版与配色由 `src/style.css` 和组件库统一管理，间距引用已有 `--space-*` 变量，不新增全局主题。表格横向滚动；调度专属布局在窄屏将固定导航改为文档流内导航，不改变其他页面。

## Interaction contracts

- 表单与下拉框、日期时间、弹窗、提示均使用 Ant Design Vue。新增/编辑使用同一表单；失败保留输入并显示错误。
- 查询显式提交，旧响应不得覆盖新查询。列表加载失败展示错误和重试，空数据有独立说明。
- 后端列表无总数，任务与执行采用上一页/下一页，满页后的下一页允许为空；不虚构总数。
- Handler 来自在线注册接口；选择应用后筛选 Handler。任务编码创建后不可编辑。
- 任务操作由 `JobStateMachineFactory` 约束；运行中执行不可取消，重试仅适用于 `RETRY_WAIT`，人工操作必须提供原因。
- 写入期间禁用重复提交。生命周期操作使用确认弹窗。运行时字段不进入保存请求。
- 注册路由和菜单时仅补充调度入口；退出清理仍由原动态路由机制负责。

## Implementation and compatibility

1. `types/scheduler.ts` 与 `api/scheduler.ts` 对齐后端字段、路径、HTTP 方法及统一响应。
2. `job/index.vue` 负责任务列表与生命周期，`JobEditor.vue` 负责保存契约。
3. `execution/index.vue` 负责记录与人工处理，`handler/index.vue` 展示当前在线处理器。
4. 共用分页、详情和窄屏布局组件，复用统一 Axios 和登录上下文。

本次扩展调度 Java 查询与 Handler 治理契约，仅增加筛选字段，不增加数据库迁移，不改变认证和全局权限契约，不增加依赖。真实联调要求网关将默认 `/api/scheduler/**` 转发到 `peach-scheduler` 的 `/scheduler/**`。

## Verification and limits

验证前端类型与生产构建、仓库统一门禁以及浏览器界面交互。模拟接口验证只证明浏览器行为与请求形状，真实网关、Quartz、RocketMQ、Handler 执行和审计仍需在线环境验证。构建产物大包提示沿用当前工程状态，不在本次进行全局打包调整。

## 当前约定

Handler 仅保留心跳在线状态，默认查询全部记录，包括离线记录，不提供手工启用开关。后端按应用、在线状态、关键字筛选完整数据集。任务仍使用既有生命周期启用和禁用。移除前轮 Handler ENABLED 字段依赖；已存在的数据库冗余列可保留，不影响运行，不执行破坏性删列。

authSchedulerDemo 使用 Cron，settingSchedulerDemo 使用固定周期；服务通过主 application.yml 自动注册 Handler，真实管理 API 脚本创建任务。执行卡片和任务列表的筛选均通过后端查询，原始响应不得覆盖后续查询结果。
