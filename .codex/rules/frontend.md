# Frontend Engineering Rule

适用于 `peach-cloud-front`。

- 使用 Vue 3 Composition API、`<script setup lang="ts">`、严格 TypeScript、Vite、Pinia、Axios、Vue Router 与项目当前 UI 技术栈。
- 页面按 `src/views/<module>/<page>/index.vue` 组织，目录使用小写 kebab-case；共享组件、composable、store 只有在存在真实复用或独立职责时抽取。
- 后端是业务路由、菜单、按钮/API 权限的事实源；前端隐藏只改善体验，不能代替后端鉴权。
- 后端返回的组件标识只能映射到本地白名单，禁止拼接并执行任意模块路径。
- Axios 统一经过项目实例、拦截器和错误归一化；页面不散落 base URL、认证头和重复状态码处理。
- Pinia 只保存跨组件/跨页面或需持久化状态；局部表单、弹窗、loading 保持组件内状态。
- 不使用 `any`、非空断言或强转掩盖未知数据；边界输入先校验再收窄。
- UI 修改必须覆盖 loading、empty、error、disabled、permission、responsive、keyboard/focus 等真实状态，不以静态截图作为完成标准。
- 只在视觉/交互/设计系统任务中叠加设计 Skill；纯 API、类型、Store、路由权限、构建修复不加载额外设计上下文。
