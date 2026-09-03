# 配置说明

本文汇总 `peach-manual` 与 `peach-cloud-front` 集成所需的全部配置项。

## 一、peach-manual 环境变量

文件位置：`peach-manual/.env.development`、`peach-manual/.env.production`

| 变量 | 开发默认值 | 生产默认值 | 说明 |
| --- | --- | --- | --- |
| `VITE_MANUAL_AUTH_ENABLED` | `false` | `true` | 是否启用登录门禁 |
| `VITE_MANUAL_API_BASE` | `/api` | `/api` | 校验 Token 的 API 前缀 |
| `VITE_MANUAL_LOGIN_PATH` | `/login` | `/login` | 未登录时跳转路径 |

### 鉴权工作原理

1. 页面加载时 `AuthGate` 读取 `peach.auth.session.v1`
2. 携带 `Authorization` 请求 `GET /api/auth/profile`
3. 返回 200 则展示手册；401 或无 Token 则跳转 `/login?redirect=...`

> Sa-Token 当前配置为 `is-read-header: true`、`is-read-cookie: false`，因此 **不能** 仅靠 nginx 静态拦截完成鉴权，必须保留客户端门禁。

### 本地跨端口说明

| 地址 | 说明 |
| --- | --- |
| 主应用 dev `5173` | 登录会话写入此 origin 的 storage |
| 手册 dev `5174` | 不同 origin，**无法**读取 5173 的 storage |

因此本地维护默认 **关闭鉴权**；若需验证登录流程，请使用同域 nginx 联调或部署到 `/manual/` 后再测。

## 二、peach-cloud-front 环境变量

建议在 `peach-cloud-front/` 下新增：

**`.env.development`**

```properties
VITE_MANUAL_BASE_URL=http://localhost:5174/manual/
VITE_MANUAL_ENABLED=true
```

**`.env.production`**

```properties
VITE_MANUAL_BASE_URL=/manual/
VITE_MANUAL_ENABLED=true
```

| 变量 | 说明 |
| --- | --- |
| `VITE_MANUAL_BASE_URL` | 手册按钮跳转基址，必须以 `/` 结尾或使用完整 URL |
| `VITE_MANUAL_ENABLED` | 设为 `false` 可隐藏右上角手册按钮 |

## 三、VitePress 站点配置

文件：`.vitepress/config.ts`

| 配置项 | 值 | 说明 |
| --- | --- | --- |
| `base` | `/manual/` | 必须与 nginx 路径一致 |
| `themeConfig.appearance` | `false` | 使用自定义三档主题切换（见下） |
| `vite.server.port` | `5174` | 本地 dev 端口 |
| `vite.server.proxy['/api']` | `18080` | 本地校验 Token 时转发网关 |
| `vite.server.proxy['/login']` | `5173` | 未登录跳转主应用登录页 |

修改导航 / 侧边栏：编辑 `themeConfig.nav` 与 `themeConfig.sidebar`。

## 四、nginx 配置（生产 / 联调）

在 peach-front 或 devops-nginx 增加：

```nginx
location /manual/ {
    alias /var/www/peach-manual/;
    try_files $uri $uri/ /manual/index.html;
}
```

要求：

- 手册静态目录指向 `peach-manual/.vitepress/dist/` 的部署副本
- `/api` 保持现有 gateway 反代（与主应用共用）
- 主应用 SPA 的 `location /` 不要拦截 `/manual/` 前缀

### 部署目录示例

```text
/var/www/peach-front/     # 主应用 dist
/var/www/peach-manual/    # 手册 dist（含 index.html 与 assets）
```

## 五、构建命令

```bash
# 手册
cd peach-manual && npm ci && npm run build

# 主应用
cd peach-cloud-front && npm ci && npm run build
```

CI 中建议并行构建，产物分别拷贝。

## 六、访问方式汇总

| 方式 | URL | 鉴权 |
| --- | --- | --- |
| 维护写作 | `http://localhost:5174/manual/` | 关闭 |
| 主应用按钮 | 新标签打开 `VITE_MANUAL_BASE_URL` | 开启（生产） |
| 直接访问 | `https://域名/manual/` | 开启 |
| 未登录访问 | 同上 | 跳转登录页 |

## 七、主应用集成点

| 文件 | 作用 |
| --- | --- |
| `src/utils/manual.ts` | `openPeachManual()` 打开手册 |
| `src/utils/auth-storage.ts` | `mirrorSessionStorageForNewTab()` 供新标签读取会话 |
| `src/layouts/admin/index.vue` | 右上角「用户手册」按钮 |

打开手册前若会话仅在 `sessionStorage`，会自动镜像到 `localStorage`，避免新标签读不到 Token。

## 主题切换

顶栏提供 **三档主题**（组件：`ManualThemeSwitch.vue`）：

| 选项 | 说明 |
| --- | --- |
| 浅色 | 对齐 `console-shell` 浅色控制面（默认） |
| 深色 | 对齐 workspace 深色 Navy 风格 |
| 自动 | 读取 OS `prefers-color-scheme` |

偏好保存在 `localStorage` 键 `peach-manual-theme`。首屏脚本在 `config.ts` 的 `head` 中注入，避免闪烁。

## 视觉与主应用对齐

| 元素 | 主应用来源 | 手册实现 |
| --- | --- | --- |
| 页面背景 | `.console-shell` `#f5f8fc` | `--pc-bg-canvas` |
| 顶栏 / 侧栏 | `.console-topbar` / `.console-sidebar` | `ManualTopbar.vue` / `ManualSidebar.vue` |
| 品牌 Logo | `PeachCloudLogo.vue` | `public/logo-mark.svg` |
| 主按钮 | `.primary-button` 渐变 | `--vp-button-brand-bg` |
| 手册按钮 | `.console-manual` | 首页 alt 按钮 + 卡片 hover |
| 激活菜单 | `.console-nav__item.is-active` | `--pc-active-bg` / `#edf4ff` |

样式维护详见 [维护指南 · 调整视觉风格](/meta/maintenance#调整视觉风格)。

## 八、可选后续增强

- nginx `auth_request` + HttpOnly Cookie（需改登录链路）
- 专用轻量接口 `GET /api/auth/session/ping`
- Changelog 从 git tag 自动生成草稿

当前版本不依赖上述增强即可投入使用。
