---
name: using-peach-front
description: 修改或审查 peach-cloud-front 的 Vue 3、TypeScript、Vite、Axios、Pinia、Vue Router、Ant Design Vue、动态路由/权限、页面结构、共享组件、状态与 UI/UX 行为时使用。
---

# Peach Frontend

先确认 `package.json` 和受影响目录的真实依赖与当前实现，再读取真正需要的 reference：目录/命名用 `references/structure-and-naming.md`；动态路由/权限/Axios 用 `references/routing-and-permissions.md`；Vue/TypeScript/Pinia/UI 编码用 `references/coding-style.md`。

## 核心不变量

- 页面采用 `src/views/<module>/<page>/index.vue`；不要重新引入扁平 `*View.vue` 风格。
- 后端是业务菜单、路由和资源权限事实源；前端只维护启动所需最小静态路由。
- 后端组件标识必须映射到 `import.meta.glob` 等本地白名单，禁止任意动态模块执行。
- Axios 统一入口和错误归一化；Pinia 只保存真实跨页面/持久化状态。
- 不以 `any`、非空断言、ignore 指令隐藏边界类型问题。
- UI 行为同时考虑 loading、empty、error、disabled、permission、responsive 和 keyboard/focus。

视觉/交互重设计才按需叠加 `ui-ux-pro-max`；设计 token/系统化组件规范才叠加 `design-system`；纯 API、类型、Store、权限、构建修复不要加载设计 Skill。

需要 README/前端架构文档时进入 `project-doc-engineer`。
