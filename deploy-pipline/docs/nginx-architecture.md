# Nginx 与入口分层方案

## 结论

**一个 Nginx 实例可以保留**，但应 **按域名职责拆分**，不要把 Jenkins / GitLab / Nacos / Nexus 等业务无关路径挂到 `peach_cloud.peachsoft.com` 下。业务域只承载前端与 API；DevOps 中间件各走独立子域；Maven 等 CLI 工具优先 **直连服务端口**，不经 HTTP 反向代理。

## 当前推荐拓扑

```text
┌─────────────────────────────────────────────────────────────┐
│  业务入口（用户 / 联调）                                      │
│  peach_cloud.peachsoft.com:80                               │
│    └─ devops-nginx → peach-front → /api → peach-gateway      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  DevOps 运维入口（开发者书签，独立 server_name）              │
│  jenkins.peachsoft.com   → jenkins:8080                     │
│  gitlab.peachsoft.com    → gitlab:80                        │
│  registry.peachsoft.com  → registry-ui / registry:5000    │
│  nacos.peachsoft.com     → nacos:8848                       │
│  nexus.peachsoft.com     → nexus:8081（浏览器 UI）          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  CLI / CI 直连（不经 DevOps Nginx）                           │
│  Maven 本地：http://nexus.peachsoft.com:8081/repository/... │
│  Jenkins 容器：http://nexus:8081/repository/...              │
│  docker push：localhost:5000                                 │
└─────────────────────────────────────────────────────────────┘
```

## 为什么不建议「一个域名走天下」

| 问题 | 说明 |
| --- | --- |
| 职责混淆 | 前端域出现 `/jenkins`、`/nacos` 会让权限边界、缓存策略、WAF 规则难以分离 |
| 路径冲突 | 前端 SPA 的 history 路由可能与 `/nacos` 等前缀冲突 |
| 安全面扩大 | 业务域一旦暴露 DevOps 控制台，攻击面与 Cookie 域混在一起 |
| 代理链路过长 | Maven 经 Nginx 再转 Nexus 增加超时、大文件上传失败风险 |
| 升级/重启影响 | 改 DevOps Nginx 配置可能误伤前端入口 |

## 已实施的调整

`devops.conf` 中 **`peach_cloud.peachsoft.com` 仅保留 `/` → peach-front**，已移除：

- `/jenkins`、`/gitlab`、`/registry` 跳转
- `/nacos/` 反代

DevOps 控制台请直接使用独立子域（hosts 中配置 `127.0.0.1 jenkins.peachsoft.com` 等）。

## 三层模型（本地 → 生产可演进）

### L1 业务网关（面向用户）

- 域名：`peach_cloud.peachsoft.com`（生产可换正式域名）
- 内容：静态前端 + `/api` → `peach-gateway`
- 未来：TLS、限流、WAF、只暴露 443

### L2 DevOps 网关（面向研发运维）

- 域名：`jenkins.peachsoft.com`、`gitlab.peachsoft.com`、`registry.peachsoft.com`、`nacos.peachsoft.com`、`nexus.peachsoft.com` 多个 `server_name`（当前 `devops-nginx`）
- 内容：Jenkins / GitLab / Registry UI / Nacos UI / Nexus UI
- 建议：仅内网 / VPN / hosts 可达，不与公网业务域混用

### L3 直连端口（面向 CLI / CI）

- Nexus：`8081`（Maven、IDE）
- Registry：`5000`（docker push/pull）
- Jenkins：`8080`（Webhook 调试）
- 优点：协议简单、大文件稳定、少一层 proxy header 问题

## Maven 与 Nginx 的关系

| 访问方式 | URL | 适用 |
| --- | --- | --- |
| **推荐** | `http://nexus.peachsoft.com:8081/repository/maven-public/` | 本地 `settings.xml` |
| CI 容器 | `http://nexus:8081/repository/maven-public/` | Jenkins 由 `settings.xml` 自动生成的 `settings.generated.xml` |
| 仅浏览 UI | `http://nexus.peachsoft.com` | 浏览器 |

settings 中 `nexus-public` 仓库 id 与 pom / server id 对齐即可；第三方依赖可通过 `aliyun-public` 回退，**不必**把 Nexus 再套一层 Nginx 给 Maven 用。仓库只维护 `../maven/settings.xml` 一份 Maven 模板，CI 不再维护单独 settings 文件。

## 生产环境进一步拆分（可选）

当上线后建议：

1. **业务 Nginx / 网关** 与 **DevOps Nginx** 拆成两台或两个 Compose stack。
2. DevOps 栈仅内网 VPC 可见；业务栈公网暴露。
3. Nacos 生产集群不对开发者浏览器开放，仅服务间 `nacos:8848`。
4. Nexus 仅 CI 网段可写；读可经 group + 国内上游 proxy。

## hosts 示例（Windows）

```text
127.0.0.1 jenkins.peachsoft.com
127.0.0.1 gitlab.peachsoft.com
127.0.0.1 nacos.peachsoft.com
127.0.0.1 registry.peachsoft.com
127.0.0.1 nexus.peachsoft.com
127.0.0.1 peach_cloud.peachsoft.com
```

## 相关文件

- `deploy-pipline/pipline/nginx/devops.conf` — DevOps 多域名反代
- `deploy-pipline/nginx/peach.conf.template` — 业务前端容器内 `/api` 模板
- `../maven/settings.xml` — Maven 唯一模板；本地复制使用，CI 自动渲染
