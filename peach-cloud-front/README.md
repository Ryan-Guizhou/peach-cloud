# peach-cloud-front

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
类型：前端工程  
技术栈：Vue 3、Vite、TypeScript、Pinia、Vue Router、Ant Design Vue、Axios

## 模块定位

`peach-cloud-front` 是 Peach Cloud 的前端工程目录，用于承载管理端或业务前端代码。该目录独立于后端 Maven reactor，使用 npm 和 Vite 构建。

本模块解决：

- 前端页面、路由、状态管理和接口调用。
- 与 `peach-gateway` 统一入口进行前后端联调。
- 前端开发、构建和预览。

本模块不解决：

- 后端服务编译和启动。
- 生产 Nginx、CDN、证书和静态资源发布。
- 后端认证、权限和接口业务逻辑。

## 目录结构

```text
peach-cloud-front
├── public/
├── src/
├── index.html
├── package.json
├── package-lock.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
└── vite.config.ts
```

## 脚本命令

当前 `package.json` 提供：

| 命令 | 说明 |
| --- | --- |
| `npm run dev` | 启动 Vite 开发服务 |
| `npm run build` | 执行 `vue-tsc -b` 后进行 Vite 构建 |
| `npm run preview` | 预览构建产物 |

## 本地开发

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

构建：

```bash
npm run build
```

预览：

```bash
npm run preview
```

## 联调约定

- 前端接口应优先通过 `peach-gateway` 访问后端服务。
- 网关本地 Docker Compose 端口为 `18080`，具体代理配置以 `vite.config.ts` 为准。
- 登录态、Token、租户、语言等上下文不要写死生产值。
- 接口路径变化时同步检查网关路由和后端 REST 前缀。

## 边界与限制

### 任务调度

- 登录后可从“任务调度”进入任务管理、执行记录和 Handler 列表。当前阶段这些入口与按钮不按调度权限码过滤，其他模块权限逻辑不变。
- 页面通过统一 Axios 请求 `/api/scheduler/**`（默认 base URL），网关需将请求转发至 `peach-scheduler` 的 `/scheduler/**`。开发代理仍使用 `vite.config.ts` 中的网关地址。
- 保存任务需要在线 Handler；新增任务为草稿。操作按钮按后端状态约束禁用，人工重试和取消必须填写原因。
- 后端分页接口返回数组且没有总数，因此任务和执行记录采用上一页/下一页导航；满页后的下一页可能为空。
- 完整联调需要运行网关、认证、调度服务及其 MySQL、Redis、Nacos、RocketMQ 依赖，并启动注册 Handler 的业务服务。前端构建通过不代表执行链路联调完成。

- 根目录 Maven 构建不会自动执行前端构建。
- `package-lock.json` 已存在，建议使用 npm 保持锁文件一致。
- 生产部署需要单独配置静态资源服务器、缓存策略、反向代理和 HTTPS。
- 前端路由权限展示不等于后端鉴权，后端接口仍需要服务侧保护。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `npm install` 失败 | Node/npm 版本、网络、锁文件是否一致 | 使用项目约定 Node 版本，必要时清理本地缓存后重装 |
| `npm run build` 失败 | TypeScript 类型、Vue 组件、依赖版本 | 根据 `vue-tsc` 输出定位源码问题 |
| 接口 404 | Vite 代理、网关端口、后端路由是否一致 | 检查 `vite.config.ts`、网关 `18080` 和后端服务 |
| 登录态异常 | Token 存储、请求头、网关鉴权是否匹配 | 对照认证接口和网关过滤器配置 |
| 页面空白 | 浏览器控制台、路由、构建资源路径 | 查看控制台错误和 Vite 构建输出 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。

### 服务调度测试与筛选

Handler 保留在线和离线记录，只展示心跳状态；支持应用、关键字、在线状态的服务端筛选，不再提供 Handler 启用开关，也不需要相关数据库迁移。任务本身仍可启用/禁用。执行卡片支持执行 ID、任务编码、状态、触发来源、执行器及计划时间范围筛选。

在 IDEA 中使用现有应用入口和正常的 `dev`（或 `docker`）环境启动 auth、setting。本地 application YAML 保留启动与 Nacos 导入配置；调度、虚拟线程组、Outbox 和 RocketMQ 由 Nacos 管理。服务自动登记 `authSchedulerDemo` 和 `settingSchedulerDemo`，与控制面共用同一基础设施环境。测试 Handler 只返回成功，不修改业务数据。

待两个 Handler 显示在线后，在任务管理查看自动登记的任务：auth 每分钟 Cron、setting 每 60 秒固定周期。测试前检查集中配置维护的初始状态。实际注册和执行需在线环境验证，构建或模拟响应不能证明真实执行链路成功。


## 任务自动登记

服务通过 `@PeachJob` 提供稳定的 `jobCode`、`jobName`，在 Nacos 的 `peach.scheduler.executor.jobs[handlerName]` 提供完整默认任务定义，包括周期、时区、误触发与并发策略、超时、重试、参数和初始状态。控制面首次登记时校验定义并创建任务；元数据缺失时只登记 Handler 和刷新心跳，不创建占位草稿。后续心跳不覆盖管理端已维护的周期、参数、启停或删除状态。同一 Handler 仍可关联其他任务。初始状态由集中配置维护。

## 功能定义

`/function` 使用左侧功能树、右侧资源表格布局，支持功能层级维护以及选中功能资源的筛选、分页和增删改。功能编码创建后只读；存在下级或引用时禁止删除。资源操作沿用 `resource:*` 权限。

已有环境需执行 [功能定义迁移](../sql/migrations/20261122_function_definition.sql)，随后重新登录，刷新菜单和网关权限快照。树形数据回归测试：`node --experimental-strip-types --test src/views/system/function/tree.test.mjs`（Node.js 24）。

浏览器回归：先构建，再运行 `python scripts/function-definition-smoke.py`，需要 Chrome 与 Python `websocket-client`。脚本仅使用本机 API 测试数据，检查树选择、筛选/错误恢复、表单校验、资源增删改和窄屏布局。

## 登录日志与导航

`/login-log` 提供时间、完整账户、结果、事件和IP筛选，以及只读详情。未归属事件仅平台审计权限可见。侧栏按后端目录分组；主动退出先调用服务端登出接口。升级与后端边界见 [登录审计](../peach-auth/docs/login-audit.md)。浏览器回归：`python scripts/login-audit-smoke.py`（先构建，需要 Chrome 与 Python `websocket-client`）。

## 角色工作区与路由列表

角色列表、功能树、资源授权采用三列布局；筛选保留隐藏授权，校准先预览再显式保存，成员与默认菜单独立维护。路由列表提供模块编码、路由编码、URL 组合查询，不展示鉴权和缓存列。后端接口及迁移见 [管理工作区](../peach-auth/docs/admin-workspace.md)。浏览器回归：构建后运行 `python scripts/admin-workspace-smoke.py`（隔离 API fixtures）。
