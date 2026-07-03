# peach-cloud-front

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
类型：前端工程  
技术栈：Vue 3、Vite、TypeScript、Pinia、Vue Router、Ant Design Vue、Axios

## 模块定位

`peach-cloud-front` 是 Peach Cloud 的前端工程目录，用于承载管理端或业务前端代码。该目录独立于后端 Maven reactor，使用 npm 和 Vite 构建。

本模块解决：

- 前端页面、路由、状态管理和接口调用。
- 与 `peach-gateway` 统一入口进行前后端联调。
- 前端开发、构建和预览。

本模块不解决：

- 后端服务编译和启动。
- 生产 Nginx、CDN、证书和静态资源发布。
- 后端认证、权限和接口业务逻辑。

## 目录结构

```text
peach-cloud-front
├── public/
├── src/
├── index.html
├── package.json
├── package-lock.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
└── vite.config.ts
```

## 脚本命令

当前 `package.json` 提供：

| 命令 | 说明 |
| --- | --- |
| `npm run dev` | 启动 Vite 开发服务 |
| `npm run build` | 执行 `vue-tsc -b` 后进行 Vite 构建 |
| `npm run preview` | 预览构建产物 |

## 本地开发

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

构建：

```bash
npm run build
```

预览：

```bash
npm run preview
```

## 联调约定

- 前端接口应优先通过 `peach-gateway` 访问后端服务。
- 网关本地 Docker Compose 端口为 `18080`，具体代理配置以 `vite.config.ts` 为准。
- 登录态、Token、租户、语言等上下文不要写死生产值。
- 接口路径变化时同步检查网关路由和后端 REST 前缀。

## 边界与限制

- 根目录 Maven 构建不会自动执行前端构建。
- `package-lock.json` 已存在，建议使用 npm 保持锁文件一致。
- 生产部署需要单独配置静态资源服务器、缓存策略、反向代理和 HTTPS。
- 前端路由权限展示不等于后端鉴权，后端接口仍需要服务侧保护。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `npm install` 失败 | Node/npm 版本、网络、锁文件是否一致 | 使用项目约定 Node 版本，必要时清理本地缓存后重装 |
| `npm run build` 失败 | TypeScript 类型、Vue 组件、依赖版本 | 根据 `vue-tsc` 输出定位源码问题 |
| 接口 404 | Vite 代理、网关端口、后端路由是否一致 | 检查 `vite.config.ts`、网关 `18080` 和后端服务 |
| 登录态异常 | Token 存储、请求头、网关鉴权是否匹配 | 对照认证接口和网关过滤器配置 |
| 页面空白 | 浏览器控制台、路由、构建资源路径 | 查看控制台错误和 Vite 构建输出 |
