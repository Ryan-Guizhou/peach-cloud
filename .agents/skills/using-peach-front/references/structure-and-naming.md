# Structure And Naming

## Target Structure

```text
peach-cloud-front/src
├── api/                    # Axios 接口函数，按业务模块分组
├── assets/                 # 构建期静态资源
├── components/             # 跨页面复用组件，按业务域或 common 分组
├── composables/            # 可复用组合式逻辑
├── constants/              # 无运行时副作用的常量
├── directives/             # Vue 指令，包括资源权限展示指令
├── layouts/                # 布局壳组件
├── router/                 # 静态启动路由、动态路由装配、守卫
├── stores/                 # Pinia Store
├── types/                  # 跨文件共享类型
├── utils/                  # 无 Vue 状态的通用函数
└── views/
    └── auth/               # 业务模块
        ├── login/          # 具体页面
        │   └── index.vue
        ├── register/
        │   └── index.vue
        └── forgot-password/
            └── index.vue
```

只创建任务实际需要的目录，不为空目录预搭完整结构。

## File Placement

- 页面入口：`views/<module>/<page>/index.vue`，且只用 `index.vue` 作为页面文件名。
- 页面私有组件：放在页面目录的 `components/`；被同一模块多个页面复用后再提升到 `components/<module>/`。
- 全局通用组件：放在 `components/common/`，必须有清晰、稳定且与业务域无关的 API。
- 请求函数：放在 `api/<module>/`；页面不得直接创建 Axios 实例。
- 跨页面状态：放在 `stores/modules/` 或仓库既定的等价位置；不要把请求函数塞进 Store，Store 只负责编排状态。
- 仅当前页面使用的类型、常量和工具优先就近放置；确认跨页面复用后再提升到顶层目录。

## Naming

| 对象 | 规则 | 示例 |
| --- | --- | --- |
| 模块/页面目录 | lowercase kebab-case | `file-management/upload-history` |
| 页面入口 | 固定 `index.vue` | `views/file/upload/index.vue` |
| Vue 组件文件 | PascalCase | `AuthShell.vue`、`UserPicker.vue` |
| composable | `use` + PascalCase | `useRoutePermission.ts` |
| Pinia Store | 文件 kebab-case，函数 `useXxxStore` | `user-session.ts` / `useUserSessionStore` |
| API 函数 | 动词开头 camelCase | `fetchUserList`、`createRole` |
| 类型/接口 | PascalCase，避免无意义 `I` 前缀 | `LoginRequest`、`RouteResource` |
| 常量 | SCREAMING_SNAKE_CASE | `PUBLIC_ROUTE_NAMES` |
| 布尔值 | `is`、`has`、`can`、`should` 前缀 | `isLoading`、`hasPermission` |
| 事件处理函数 | `handle` + 行为 | `handleSubmit` |

组件名表达业务语义，禁止 `CommonComponent`、`DataInfo`、`TempPage` 等模糊命名。路由 `name` 使用稳定且全局唯一的业务标识，不把展示文案当作标识。

## Imports And Boundaries

- 使用配置好的路径别名；若项目尚未配置别名，沿用相对路径，不为单个文件擅自引入新别名。
- 导入顺序依次为第三方依赖、项目模块、相对路径、样式；各组之间空一行。
- 禁止页面反向依赖另一个页面的 `index.vue`。抽取共享组件或 composable 后复用。
- 避免跨模块深层导入内部文件；通过模块公开入口或稳定共享层访问。

## Legacy Migration

认证页面当前已使用 `auth/login/index.vue`、`auth/register/index.vue`、`auth/forgot-password/index.vue` 目标结构。旧分支或其他模块仍存在扁平页面时，迁移一组强关联页面前先扫描整组；没有明确任务时不要顺手移动无关页面。

## Behavior-Preserving Migration

目录迁移默认是结构改造，不授权改变已实现功能。按以下顺序执行：

1. 迁移前记录路由 `path`、`name`、`redirect`、允许的 `meta`、页面 props/emits、表单初始值、可见文案和提交/跳转行为。
2. 先机械移动页面，再只修改因目录深度变化而失效的导入。默认严格保持模式下，不要顺带调整函数/变量命名、注释、格式、UI 组件、样式或接入尚未存在的 API/Store；这些改动即使预计无运行时影响，也必须获得单独授权。
3. 同步更新静态路由导入、`import.meta.glob` 映射、测试、文档和所有旧路径引用。保持后端组件逻辑标识稳定，除非协议变更已明确授权。
4. 全仓搜索旧文件名和旧路径，确认没有运行时字符串、测试夹具或文档遗漏。
5. 比较迁移前后的完整源码、模板、状态初值和事件副作用；默认允许的差异仅为文件路径及因此必须变化的导入路径。若用户明确允许同步规范化，再单独列出命名或格式差异。
6. 执行构建，并至少冒烟验证原 URL 直达、页面间跳转、表单输入、密码显隐、提交结果与刷新行为。

构建产物 chunk 文件名可能因页面统一命名为 `index.vue` 而变化，不能把 chunk 名当作页面身份。若部署、监控或预加载配置显式依赖旧 chunk 名，必须将其纳入兼容检查并单独处理。
