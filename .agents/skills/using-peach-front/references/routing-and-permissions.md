# Routing And Permissions

## Source Anchors And Status

当前后端源码已确认：

- `peach-auth/.../LoginInfo.java` 的登录信息包含 `token`、`menuList` 和 `routerList`。
- `RouterDTO.java` / `RouterDO.java` 包含 `routerCode`、`routerName`、`routerUrl`、`filePath`、`isAuth`、`isCache`、`moduleCode`、`routerLevel`。
- `MenuDO.java` 包含菜单层级、URL、功能编码、显示状态、图标和打开方式等字段。

这些字段是现状证据，不代表前端动态路由协议已经完整实现。新增前端适配时以实际 REST 响应为准，先定义边界类型和转换函数，不猜测 `0/1`、层级、缓存或打开方式的具体语义。

## Ownership Model

- 后端是业务路由、菜单和资源权限的唯一事实来源。
- 前端静态路由只保留应用启动必需且无需登录的页面，如登录、注册、找回密码和错误页。静态白名单必须显式且最小。
- 登录成功或恢复会话后，获取当前用户上下文，校验路由与权限数据，再注册动态路由。
- 退出登录、身份切换或权限刷新时移除旧动态路由并清空权限状态，避免跨用户残留。
- 页面刷新时先恢复可信会话，再完成动态路由装配；装配期间使用明确的加载态，避免守卫循环。

## Safe Dynamic Component Resolution

使用构建期白名单解析页面组件：

```ts
const viewModules = import.meta.glob('../views/**/index.vue')

function resolveView(componentKey: string) {
  const normalizedKey = normalizeComponentKey(componentKey)
  const loader = viewModules[`../views/${normalizedKey}/index.vue`]

  if (!loader) {
    throw new Error('Unknown route component')
  }

  return loader
}
```

- `normalizeComponentKey` 只接受约定的模块/页面标识，拒绝 `..`、协议、绝对路径、查询串和反斜杠等越界内容。
- 后端最好返回稳定的逻辑组件标识（如 `auth/forgot-password`），不要把仓库物理绝对路径作为协议。
- 未命中的组件进入受控错误页并记录非敏感诊断信息；不要静默跳过导致菜单与路由不一致。
- 校验路径、名称唯一性、父子关系、重定向目标和 meta 白名单；忽略后端传入的可执行函数或任意对象。

## Permission Model

- Store 中使用 `Set<string>` 或等价结构保存资源权限码；权限码来自后端，前端不根据角色名自行推导。
- 页面级权限由路由守卫处理；按钮/区域级权限由组合函数或指令处理；请求能否成功最终由后端决定。
- “超级管理员”规则若存在，必须来自明确协议或后端返回的权限集合，禁止在多个组件中硬编码角色名。
- 无权限和未登录是不同状态：未登录跳转登录页，无权限进入 403 或业务约定页面。
- 前端不得仅凭菜单是否显示判断路由是否可访问，也不得把隐藏菜单等同于禁用接口。

## Axios Boundary

- 创建单一 Axios 客户端，集中配置网关 base URL、超时、认证头和统一响应/错误适配。
- token 只从会话状态读取；不要写入 URL、日志、错误消息或提交到源码。持久化策略必须结合威胁模型确定，不能默认宣称 localStorage 安全。
- 401 清理会话并触发一次登录流程；禁止多个并发请求重复弹窗或形成刷新 token 死循环。
- 使用 `unknown` 接收未经验证的外部响应，在 API 边界转换为领域类型；不要把 Axios 响应对象泄漏给页面。
- 请求取消、重复提交和竞态按页面风险处理；搜索列表至少保证旧响应不会覆盖新查询结果。

## Review Checklist

- 静态路由中是否出现了后端本应管理的业务权限？
- 动态组件是否通过本地白名单解析并拒绝路径穿越？
- 刷新、退出、切换用户、路由删除和 401 是否有确定行为？
- 资源权限是否来自后端，且没有把前端隐藏误当作后端鉴权？
- 是否泄露 token、完整登录响应或敏感请求数据？
