# 维护指南

本文面向 **文档维护者**（产品、实施、运维或开发），说明如何日常更新 `peach-manual` 用户手册。

## 目录结构

```text
peach-manual/
├── README.md                 # 工程总览：已做 / 待办 / 怎么维护（请先读）
├── package.json
├── .vitepress/               # 配置与 console-shell 主题（含 AuthGate）
├── guide/                    # 用户操作导引
├── changelog/                # 版本更新记录
├── faq/                      # 常见问题
├── meta/                     # 维护与配置文档
├── public/                   # Logo、截图
└── index.md                  # 首页
```

与仓库根目录 `docs/`（研发架构文档）**物理隔离**，不要混放。

> 完整「已完成 / 待办 / 路线图」见 [README.md](../README.md)。

## 本地启动（维护期默认免登录）

```bash
cd peach-manual
npm install
npm run dev
```

浏览器访问：`http://localhost:5174/manual/`

维护期 `.env.development` 中 `VITE_MANUAL_AUTH_ENABLED=false`，可直接编辑预览，无需登录。

## 日常改文档流程

1. 在对应目录新增或编辑 `.md` 文件
2. 若需调整导航 / 侧边栏，修改 `.vitepress/config.ts` 中 `nav` 与 `sidebar`
3. 图片放入 `public/images/`，引用示例：

   ```markdown
   ![授权管理示意](/manual/images/auth-demo.png)
   ```

4. 本地 `npm run dev` 预览
5. 提交 Git（与功能代码同 PR 或独立 docs PR 均可）

## 调整视觉风格

手册与主应用控制台（`console-shell`）共用视觉语言，避免从顶栏「用户手册」进入时有违和感。

| 位置 | 文件 |
| --- | --- |
| 主应用设计 token | `peach-cloud-front/src/style.css`（`.console-*`） |
| 手册设计 token | `.vitepress/theme/tokens.css`（`--pc-*` 变量） |
| 壳层布局 | `.vitepress/theme/shell.css` + `ManualLayout.vue` 等组件 |
| 正文排版 | `.vitepress/theme/doc.css` |
| 主题切换逻辑 | `.vitepress/theme/manual-theme.ts` + `ManualThemeSwitch.vue` |
| Logo | `public/logo-mark.svg`（与 `PeachCloudLogo.vue` 一致） |

主应用调整控制台配色后，请同步更新 `tokens.css` 中同名语义变量。手册默认浅色；顶栏可切换浅色 / 深色 / 自动（`appearance: false`，不使用 VitePress 内置开关）。

## 新增一篇导引

1. 在 `guide/` 创建文件，例如 `guide/file-upload.md`
2. 在 `.vitepress/config.ts` 的 `guideSidebar` 数组追加条目
3. 在相关页面增加交叉链接

## 发版更新 Changelog

1. 复制 `changelog/v1.0.0.md` 为 `changelog/vX.Y.Z.md`
2. 填写 **新增 / 变更 / 修复 / 升级注意**
3. 更新 `changelog/index.md` 版本表格（新版本置顶）
4. 在发版 checklist 中勾选「已更新 peach-manual changelog」

### Changelog 模板

```markdown
# vX.Y.Z

**发布日期：** YYYY-MM-DD

## 新增
- ...

## 变更
- ...

## 修复
- ...

## 升级注意
- ...
```

## 更新 FAQ

直接在 `faq/index.md` 追加章节，使用 `## 标题 {#anchor}` 便于站内链接。

收集渠道建议：

- 实施反馈
- 客服 / 工单
- 控制台报错截图（脱敏后）

## 截图规范

- 脱敏：不含真实 token、密码、手机号、内网 IP
- 统一浅色控制台主题截图
- 文件名：`模块-动作-序号.png`，存放于 `public/images/`

## 构建与预览

```bash
cd peach-manual
npm run build          # 输出 .vitepress/dist
npm run preview        # 本地预览生产构建（默认开启鉴权）
```

生产构建使用 `.env.production`，`VITE_MANUAL_AUTH_ENABLED=true`。

## 部署

将 `.vitepress/dist/` 内容同步到 nginx 静态目录（例如 `/var/www/peach-manual/`），并确保：

- 主应用与手册 **同域**
- nginx 配置 `location /manual/`（见 [配置说明](/meta/configuration)）

## 与主应用功能同步矩阵

| 主应用变更 | 手册需更新 |
| --- | --- |
| 新菜单 / 页面 | `guide/` 新增操作文 |
| 权限 / 授权模型 | `guide/authorization.md` + FAQ |
| 登录 / 机构切换 | `guide/login-and-context.md` |
| 破坏性升级 | `changelog` 的「升级注意」 |

## 不要做的事

- 不要把研发设计文档（`docs/permission/` 等）整篇复制到用户手册
- 不要在文档中粘贴真实 token、密码或完整请求报文
- 不要在生产环境关闭 `VITE_MANUAL_AUTH_ENABLED`

## 疑难排查

| 问题 | 处理 |
| --- | --- |
| 本地改 markdown 不刷新 | 重启 `npm run dev` |
| 图片 404 | 确认路径以 `/manual/` 开头 |
| 生产提示未登录 | 确认同域且已在主应用登录 |
| 搜索不到新页面 | 重新 `npm run build` 部署 |

更多环境变量与 nginx 细节见 [配置说明](/meta/configuration)。
