# Peach Manual（peach-manual）

Peach Cloud **用户手册**站点：基于 [VitePress](https://vitepress.dev/) + Markdown，面向业务用户、实施与运维，与仓库根目录 `docs/`（研发文档）分离。

---

## 这个手册是做什么的

| 目标 | 说明 |
| --- | --- |
| 帮助用户快速上手 | 登录、机构切换、工作台、各业务模块入口 |
| 集中 FAQ 与 Changelog | 常见问题与版本变更可检索、可链接 |
| 可集成主应用 | 控制台右上角「用户手册」新标签打开，登录后可读 |
| 可独立维护 | 本地 `npm run dev` 写 Markdown，无需改 Vue 业务代码 |

访问路径（集成后）：`https://<你的域名>/manual/`

---

## 当前已完成

### 站点工程

- [x] VitePress 工程 `peach-manual/`，`base: '/manual/'`
- [x] 自定义 `ManualLayout` 控制台壳层（256px 侧栏 + 68px 顶栏），不再覆盖 VitePress 默认布局
- [x] 视觉与 `peach-cloud-front` 控制台（`console-shell`）对齐
- [x] 使用与 `PeachCloudLogo.vue` 相同的品牌 Logo
- [x] 关闭 VitePress 默认主题开关，改用三档自定义切换（浅色 / 深色 / 自动），默认浅色
- [x] 全宽控制台布局：消除宽屏左右空白 gutter 与背景色差
- [x] 本地全文搜索（`local` provider）
- [x] 登录门禁 `AuthGate`（生产启用，开发默认关闭）
- [x] 中文导航、侧边栏、页脚

### 文档内容（第一版）

- [x] 首页角色入口卡片
- [x] [快速开始](guide/getting-started.md)
- [x] [平台功能概览](guide/platform-overview.md) — 对齐当前控制台菜单分组
- [x] [登录与机构上下文](guide/login-and-context.md)
- [x] [工作台与消息](guide/workspace.md)
- [x] [授权与数据权限](guide/authorization.md)
- [x] [文件与存储](guide/file-storage.md) — 概要级
- [x] [日志与审计](guide/logs-and-audit.md)
- [x] [常见问题](faq/index.md)
- [x] [Changelog v1.0.0](changelog/v1.0.0.md)
- [x] [维护指南](meta/maintenance.md) / [配置说明](meta/configuration.md)

### 主应用集成

- [x] `peach-cloud-front` 顶栏「用户手册」按钮
- [x] `src/utils/manual.ts` — 打开手册 + 登录跳转
- [x] `mirrorSessionStorageForNewTab()` — 新标签读取会话
- [x] `.env.development` / `.env.production` 手册地址配置

### 质量验证

- [x] `npm run build`（peach-manual + peach-cloud-front）通过
- [x] UTF-8 无 BOM 检查通过

---

## 尚未完成（待办）

### 部署与 CI

- [ ] nginx `location /manual/` 写入 `deploy-pipline` 配置并实际上线
- [ ] CI 增加 `peach-manual` 构建与产物发布步骤
- [ ] 生产环境首次部署 dist 到 `/var/www/peach-manual/`

### 文档深化（按模块补操作截图与步骤）

- [ ] 系统设置：字典 / 值集 / 多语言 / 公告 / IP 白名单 分篇指南
- [ ] 运行监控：指标说明与告警处理
- [ ] 文件存储：分片上传、Provider 字段表（可参考 `docs/FileStorage-Manual.md` 简化）
- [ ] 消息中心：消息类型与处理流程
- [ ] 各 CRUD 页面通用操作说明（可一篇「列表页通用操作」）

### 体验增强

- [ ] 手册内嵌截图（`public/images/`，需脱敏）
- [ ] 可选：nginx `auth_request` + Cookie 二次防护（需改登录链路）
- [ ] 可选：Changelog 从 git tag 自动生成草稿脚本
- [ ] 可选：多版本手册（VitePress `themeConfig.version`）

### 已知限制

- 本地 dev 手册（5174）与主应用（5173）**不同源**，无法测真实鉴权；同域部署后可测
- 静态 HTML 可被直接请求；敏感内容不应放入手册
- 部分页面仍为概要级，需随发版持续补齐

---

## 日常怎么改内容

### 1. 本地写作（推荐）

```bash
cd peach-manual
npm install          # 首次
npm run dev
```

浏览器打开：`http://localhost:5174/manual/`（本地开发地址，勿作为生产链接）

`.env.development` 中 `VITE_MANUAL_AUTH_ENABLED=false`，**无需登录**即可编辑预览。

### 2. 改正文

| 改什么 | 改哪里 |
| --- | --- |
| 首页 | `index.md` |
| 操作指南 | `guide/*.md` |
| FAQ | `faq/index.md` |
| 版本记录 | `changelog/vX.Y.Z.md` + `changelog/index.md` |
| 维护 / 配置 | `meta/*.md` |
| 图片 | `public/images/`，引用 `/manual/images/xxx.png` |

### 3. 改导航

编辑 `.vitepress/config.ts`：

- 顶栏：`themeConfig.nav`
- 侧边栏：`guideSidebar` 等数组

新增 guide 页面时：**新建 md + 在 sidebar 加一项**。

### 4. 构建与上线

```bash
cd peach-manual
npm run build
# 产物：.vitepress/dist/ → 部署到服务器 /var/www/peach-manual/
```

生产使用 `.env.production`（`VITE_MANUAL_AUTH_ENABLED=true`）。

### 5. 发版 checklist

- [ ] 功能变更是否更新对应 `guide/` 页面
- [ ] 是否新增 `changelog/vX.Y.Z.md`
- [ ] 是否更新 `changelog/index.md` 表格
- [ ] 是否更新本 README 的「已完成 / 待办」
- [ ] `npm run build` 通过后部署 dist

更细流程见站内 [维护指南](meta/maintenance.md)。

---

## 环境变量速查

### peach-manual

| 变量 | 开发 | 生产 | 含义 |
| --- | --- | --- | --- |
| `VITE_MANUAL_AUTH_ENABLED` | `false` | `true` | 是否校验登录 |
| `VITE_MANUAL_API_BASE` | `/api` | `/api` | 校验接口前缀 |
| `VITE_MANUAL_LOGIN_PATH` | `/login` | `/login` | 未登录跳转 |

### peach-cloud-front

| 变量 | 开发 | 生产 | 含义 |
| --- | --- | --- | --- |
| `VITE_MANUAL_BASE_URL` | `http://localhost:5174/manual/` | `/manual/` | 手册按钮地址 |
| `VITE_MANUAL_ENABLED` | `true` | `true` | 是否显示按钮 |

详见 [配置说明](meta/configuration.md)。

---

## 目录结构

```text
peach-manual/
├── README.md                 # 本文件：总览、待办、维护入口
├── package.json
├── .env.development
├── .env.production
├── .vitepress/
│   ├── config.ts             # 站点 / 导航 / 代理
│   └── theme/                # ManualLayout 壳层 + tokens/shell/doc.css + AuthGate
├── index.md                  # 首页
├── guide/                    # 用户指南
├── changelog/                # 版本记录
├── faq/                      # 常见问题
├── meta/                     # 维护者与配置文档
└── public/                   # 静态资源
```

---

## 后续建议怎么做（路线图）

### 短期（1～2 周）

1. 补 nginx / CI，完成生产 `/manual/` 首次部署
2. 为 **授权管理、角色授权** 补 3～5 张脱敏截图
3. 从实施 / 客服收集 5 条真实 FAQ 合并进 `faq/`

### 中期（随版本）

1. 每个发版 mandatory 更新 Changelog
2. 新菜单上线时同步新增或更新 `guide/` 一篇
3. 系统设置、监控模块各写一篇短指南

### 长期（可选）

1. 手册版本与 Peach Cloud 版本号联动
2. 关键流程录屏或 GIF（仍放 `public/`）
3. 与工单系统打通「从 FAQ 一键反馈」

---

## 相关链接

| 资源 | 位置 |
| --- | --- |
| 用户手册站点 | `/manual/` |
| 研发文档 | 仓库 `docs/` |
| 主应用 | `peach-cloud-front/` |
| nginx 架构说明 | `deploy-pipline/docs/nginx-architecture.md` |

---

## 命令速查

```bash
# 开发
cd peach-manual && npm run dev

# 构建
cd peach-manual && npm run build

# 预览生产构建（默认开启鉴权）
cd peach-manual && npm run preview

# 主应用联调
cd peach-cloud-front && npm run dev
```

---

如有疑问，先读 [meta/maintenance.md](meta/maintenance.md)；部署问题读 [meta/configuration.md](meta/configuration.md)。
