# Coding Style

## Vue And TypeScript

- 使用 `<script setup lang="ts">`、Composition API 和单向数据流；SFC 顺序为 `script`、`template`、`style`。
- Props、Emits、模板引用、API 响应和 Store 状态必须有明确类型。优先 `defineProps<T>()`、`defineEmits<T>()`，避免运行时与类型声明重复。
- 计算值使用 `computed`，副作用使用 `watch`/`watchEffect`，事件动作使用普通函数；不要用 watcher 模拟可直接表达的计算属性。
- `ref` 保存标量或需要替换的值，`reactive` 保存稳定对象；不要无理由深层响应化大型服务端数据。
- 禁止裸 `any`、大范围 `as`、`@ts-ignore` 和无解释的非空断言。第三方或后端未知数据先以 `unknown` 接收并校验。
- 异步操作必须处理 loading、成功、空数据和失败状态；在 `finally` 中恢复 loading，避免重复提交。

## Component Design

- 页面负责路由参数、数据编排和页面布局；可复用组件通过 props/emits 暴露最小接口，不直接读取页面私有 Store 状态。
- 单组件只承担一个清晰职责。重复模板不等于必须抽象；出现稳定复用语义或复杂独立交互时再抽取。
- Props 只读，禁止子组件直接修改；`v-model` 使用 Vue 3 的 `modelValue` / `update:modelValue` 或具名模型约定。
- 列表使用稳定业务主键作为 `key`，禁止在可增删排序列表中用数组索引。
- 清理定时器、事件监听、订阅和未完成请求，避免页面卸载后的状态写入。

## Ant Design Vue

- 表单使用 `a-form` 的 model、rules 和校验状态；提交前完成客户端基础校验，但不替代后端校验。
- 统一使用 `message`、`notification`、`modal` 的项目级封装或上下文方式，避免页面各自定义不一致反馈。
- 表格分页、筛选和排序参数在 API 边界转换；不要让后端 DTO 形状直接控制视图组件内部状态。
- 图标按钮必须提供可感知名称；表单控件必须有关联标签；弹窗打开后焦点可达，关闭后回到合理位置。

## Pinia

- Store 按业务能力拆分，state 保持可序列化；DOM、组件实例、Axios 响应和大型临时对象不进入 Store。
- Getter 表达派生状态，Action 处理状态转换和跨请求编排；组件不应散落修改多个 Store 字段来模拟一个业务动作。
- 持久化只保存恢复会话必需的最小非敏感数据，定义版本与清理策略；权限和动态路由在身份变化时重建。
- 不把纯服务端缓存无限复制到全局 Store；页面级查询优先就地管理，确需共享再提升。

## Formatting And Naming

- 使用 2 空格缩进、单引号、无分号风格，并保留多行结构的尾逗号；若仓库后续引入 ESLint/Prettier，以已提交配置为准。
- `package.json` 同一对象内禁止重复键。重复依赖版本相同时只保留一项；版本冲突时先确认实际锁定版本和兼容性，不擅自选择。依赖图未变化时不要无意义重写 lockfile。
- 模板属性过长时一行一个；条件复杂时移入具名 computed，禁止在模板写难以测试的长表达式。
- 注释解释约束、原因和边界，不复述代码。TODO 必须说明待办原因或跟踪项，禁止长期保留调试注释。
- 生产代码禁止遗留 `console.log`、`debugger` 和临时 mock；必要日志只输出非敏感业务标识和错误分类。

## CSS And Accessibility

- 页面样式默认使用 `scoped`；全局 reset、主题 token 和布局基础样式集中管理，不把页面选择器泄漏到全局。
- class 使用语义化 kebab-case；复合组件可采用一致的 BEM 风格，禁止用颜色或位置命名业务元素。
- 优先主题 token/CSS 变量，避免散落魔法色值、z-index 和重复间距；响应式布局从内容断点出发。
- 使用语义 HTML。可点击行为优先 `button`/`a`，不要用 `div` 模拟；确保键盘可操作、焦点可见、文本对比合理。
- 动画尊重 `prefers-reduced-motion`；图片提供合适的 `alt`，装饰图使用空 `alt` 或从可访问树隐藏。

## Security And Correctness

- 禁止 `v-html` 渲染不可信内容；确有富文本需求时使用经过确认的清洗方案并说明信任边界。
- 不在前端源码、环境示例、日志或测试快照中放真实 token、密码、secret、签名 URL 或个人敏感信息。
- `.env` 变量会进入浏览器构建产物，不能存放服务端秘密；只暴露允许公开的运行时配置。
- 错误提示对用户清晰但不暴露堆栈、内部地址和认证细节；完整技术诊断留在受控环境。

## Verification

在 `peach-cloud-front` 运行：

```bash
npm run build
```

若仓库增加 lint、unit 或 e2e 脚本，按改动风险一并执行。构建通过只证明类型检查和打包成功；动态权限仍需至少验证登录、刷新、无权限、退出和身份切换路径。
