# 登录与机构上下文

Peach Cloud 使用 **Sa-Token** 管理登录会话。登录成功后，后端会装配 **权限快照**（菜单、路由、按钮资源、数据权限等），前端据此渲染控制台。

## 登录流程概览

```text
输入账号密码 + 验证码
    ↓
POST /api/auth/login
    ↓
返回 LoginInfo（含 token 与 permissionSnapshot）
    ↓
前端写入 localStorage 或 sessionStorage
    ↓
动态注册路由 + 渲染菜单
```

## 会话存储

| 存储位置 | 触发条件 | 新标签页共享 |
| --- | --- | --- |
| `localStorage` | 勾选「记住登录」 | 同域下可共享 |
| `sessionStorage` | 未勾选记住登录 | 默认不跨标签 |

存储键名（与主应用一致）：

- `peach.auth.session.v1` — 会话（token、用户、机构、过期时间）
- `peach.auth.loginInfo.v1` — 登录信息与权限快照

请求 API 时，前端在 Header 中携带：

```http
Authorization: <token>
```

与 Nacos 中 `sa-token.token-name: Authorization` 配置一致。

## 机构上下文

一个用户可关联多个 **租户 / 机构**。右上角 **选择机构上下文** 会：

1. 调用后端切换接口
2. 重新生成权限快照
3. 刷新动态路由

切换后建议回到 **工作台** 再进入目标功能，避免旧页面状态干扰。

## 个人中心与退出

- **个人中心**（`/profile`）：维护姓名、联系方式、头像
- **退出**：清理本地会话、断开 WebSocket、跳转登录页

## 与用户手册的关系

集成环境下，手册站点（`/manual/`）通过相同存储键读取会话，并请求 `GET /api/auth/profile` 校验。

| 要求 | 说明 |
| --- | --- |
| 同域部署 | 手册与主应用须同一域名，否则无法共享 storage |
| 新标签打开 | 主应用在打开手册前会将 sessionStorage 镜像到 localStorage |
| 本地 dev 跨端口 | `5173` 与 `5174` 不共享 storage；维护期默认关闭手册鉴权 |

详见 [配置说明](/meta/configuration)。

## 相关文档

- [快速开始](/guide/getting-started)
- [授权与数据权限](/guide/authorization)
- [FAQ：手册提示未登录](/faq/#manual-auth)
