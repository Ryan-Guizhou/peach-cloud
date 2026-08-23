---
name: using-peach-front
description: 约束 peach-cloud-front 的 Vue 3、TypeScript、Vite、Axios、Pinia、Ant Design Vue 与 Vue Router 代码编写、迁移和审查；用于新增或修改页面、组件、路由、权限、状态、接口请求、样式、响应式、可访问性、UI/UX 设计审查及前端目录结构时，并按任务类型协调 ui-ux-pro-max、design-system 或 ui-styling。
---

# Peach Frontend

## Workflow

1. 读取 `peach-cloud-front/package.json`、TypeScript/Vite 配置和受影响目录，确认当前依赖与构建能力，不凭通用 Vue 项目假设生成配置。
2. 判断任务涉及页面结构、路由权限还是编码风格，只读取对应 reference；跨层任务读取所有相关 reference。
3. 扫描同模块实现，区分本 skill 的目标规范与存量兼容。安全、正确性和用户明确要求高于历史写法。
4. 设计最小改动范围；改动路由协议、公共请求响应、共享组件或 Store 前先检查全部调用方。
5. 实现后执行类型构建、UTF-8 和差异门禁，并报告未验证项。

## Required Rules

- 使用 Vue 3 Composition API、`<script setup lang="ts">` 和严格 TypeScript；禁止用 `any`、非空断言或类型强转掩盖未知数据，确有边界适配时先校验再收窄。
- 页面统一放在 `src/views/<module>/<page>/index.vue`。`module` 和 `page` 使用小写 kebab-case，例如 `src/views/auth/forgot-password/index.vue`。
- `views` 通过模块目录区分业务域，再通过页面目录区分页面；禁止新增 `ForgotPasswordView.vue`、`UserList.vue` 等扁平页面文件。
- 后端返回登录用户可用的路由、菜单和资源权限；前端只保留登录、错误页等启动所需的最小静态路由，不在源码中维护业务权限真相。
- 将后端组件标识解析到 `import.meta.glob` 生成的本地组件白名单。禁止直接拼接或执行后端返回的任意模块路径。
- 路由可见性和接口授权是两层控制。前端隐藏按钮或菜单只改善体验，后端仍必须独立鉴权。
- Axios 请求经过统一实例、拦截器和错误归一化；禁止页面内散落 base URL、认证头、重复状态码处理或真实凭据。
- Pinia 只承载跨组件、跨页面或需持久化的状态；一次性表单、弹窗和加载态保留在组件内。
- 优先使用 Ant Design Vue 的可访问组件完成表单、反馈和数据展示；定制视觉不能破坏键盘操作、焦点、标签和错误提示。
- 所有文本文件使用 UTF-8 无 BOM；不要记录 token、密码、完整认证响应、个人敏感信息或完整请求对象。

## References

- 页面目录、模块边界、导入与文件命名：读取 [structure-and-naming.md](references/structure-and-naming.md)。
- 动态路由、菜单、资源权限和 Axios 边界：读取 [routing-and-permissions.md](references/routing-and-permissions.md)。
- Vue、TypeScript、Pinia、Ant Design Vue、样式与质量规则：读取 [coding-style.md](references/coding-style.md)。

## UX/UI Skill Coordination

先应用本 skill 的 Vue 3、Ant Design Vue、目录、权限和构建边界，再按任务真实范围叠加最小必要的 UX/UI skill：

| 任务信号 | 叠加 skill | 调用时机 |
| --- | --- | --- |
| 新页面视觉方案、交互流程、布局重构、响应式行为、动效或体验优化 | `ui-ux-pro-max` | 实现前形成设计决策，完成前复核可访问性和体验质量 |
| 设计 token、主题变量、间距/字体尺度、组件状态规范或系统化组件库 | `design-system` + `ui-ux-pro-max` | 先确定 token/组件规范，再按本项目技术栈实现 |
| 用户要求审查 UI、检查可访问性、审计 UX 或对照 Web 最佳实践 | `ui-ux-pro-max` | 只读审查时输出问题，不自动修改；用户要求修复时再按本 skill 实现 |
| 用户明确要求 Tailwind、shadcn/ui、Canvas 视觉方案，或任务只需其框架无关的视觉方法 | `ui-styling` | 先确认与 Vue 3 + Ant Design Vue 的兼容边界；未经明确授权不得引入 React、shadcn/ui 或 Tailwind |

以下任务不要自动叠加 UX/UI skills：仅移动目录、保持行为的重命名、纯 API/类型/Store/路由权限逻辑、依赖维护、构建修复或不改变界面外观与交互的代码整理。用户明确点名某个 skill 时按用户要求使用，但仍遵守本项目技术栈和安全边界。

认证页面已采用 `src/views/auth/<page>/index.vue` 目标结构，不得重新引入 `LoginView.vue` 等扁平页面。处理旧分支或其他模块的扁平页面时，按 structure reference 的行为保持迁移流程执行。

## Definition Of Done

- 页面、组件、API、Store 和类型位于正确层级，命名符合 reference。
- 动态路由只接受校验后的后端数据，组件解析受本地白名单约束，刷新和无权限场景有确定行为。
- 无敏感信息泄露、无无障碍明显回退、无以 `any` 或忽略指令逃避类型检查。
- 在 `peach-cloud-front` 执行 `npm run build`。
- 在仓库根目录执行 `node scripts/check-utf8.mjs` 和 `git diff --check`。
- 无法执行的检查及残余风险在最终回复中明确说明。
