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

### SaaS IAM 前端安全基线（#210，增量）

动态业务路由**只来自 Auth 返回的 routerList**：不再因授权路由为空而退回本地预定义管理路由。菜单只有后端返回并匹配的项目，无授权时显示独立的受保护 `/no-access` 页面，不会虚构管理员权限。默认登录跳转在没有 `/workspace` 授权路由时落到该页面。工作台与管理布局的登出都会先尝试 `POST /auth/logout`，即使网络故障也在 `finally` 中清理所有本地 Token、旧动态路由和 WebSocket 会话。已对齐新的 `year/tenantRefId/memberRefId/appRefId` 登录契约，支持密码认证后选择租户与应用、用户自主选择自然年度、无机构成员登录及二次提交。会话存储拒绝旧 `fiscal` 和缺失物理身份的数据。头部支持已登录切换租户/应用/年度，切换成功后清除旧路由和 WebSocket 订阅；若服务端已切换而前端状态更新失败，则强制清理会话并要求重新登录，避免跨租户保留旧界面。**仍未完成机构、成员、角色以及行/字段授权管理页面的全部业务迁移，且后端当前可能返回空权限快照，因此保持 Draft；不能仅凭 Vue build 认定 #210 已验收。**

服务端有菜单但缺少对应授权路由时，前端也不再根据本地组件候选路由推断出可点击的管理菜单；路由的访问事实只来自当前用户的 `routerList`，原 `fallbackAdminRoutes` 仅用于将已授权的路由映射到构建期本地组件，不代表授予资源权限。

### SaaS IAM 角色写入契约（#210，增量）

新增 `src/api/saas-iam.ts`，基于实际 `SaasRoleController` 与 `SaasPolicyController` 的受保护接口提供类型化方法：创建当前年度租户角色、替换角色权限、成员独立角色范围绑定/撤销、替换行级与字段读写策略。只接受后端目录选择得到的 32 位物理 UUID 和操作字段，拒绝旧 `tenantId/orgId/fiscal/ROLE_CODE` 拼接关联；作用域组合、角色业务码、资源码及批量上限均提前校验，但**最终管理权限、租户和年度必须由 Auth 服务再次按 Token 验证**。目前缺少安全的角色/成员/权限目录分页查询接口，因此前端授权管理表单尚未全面切换到这些写 API；不要让管理员手动猜测或输入物理外键。保留此任务 Draft，等目录查询和业务 UI 接入后验收。

### SaaS 角色管理页面（#210，增量）

旧的 `/auth/role/add` 和 `/auth/authFunction/saveRoleFunctions` 等基于 `tenantId/orgId/fiscal`、角色业务码拼接的前端写入接口已经从 `api/admin.ts` 移除。角色页面改为通过 `createTenantRole()` 执行当前 Token 范围内的受控新建，真实租户/应用/年度完全由 Auth 决定；物理 ID 仅作为返回值展示，不允许手工录入。权限授权页面不再允许旧多模型请求提交，需等待安全的后端角色/成员/权限分页目录补齐后再接入新的 `saas-iam.ts` 写接口。**这属于明确的安全收敛，未完成完整管理界面，不得视为 #210 已交付。**

### #227 SaaS 角色授权可操作界面

`src/api/saas-iam.ts` 新增受保护的 `fetchSaasCatalog`、`fetchRolePermissionRefs` 和 `fetchMemberBindings`。年度角色授权页不再是占位信息，已经接入角色/权限/成员/机构分页选择器；支持替换当前租户年度角色的全部权限、对成员按 `TENANT/ORG/ORG_TREE` 独立授权和撤销**指定物理绑定**。可委派权限未加载完或角色仍持有当前管理员不可见的权限时，全量替换被明确禁止，避免误删授权。所有 API 请求依赖当前 Token，页面不能编辑 `tenantRefId/appRefId/year`。此项仍需真实数据库、网关在线 IAM 与旧数据迁移测试；行/字段策略编辑 UI 需要后续受控字段目录支持。
