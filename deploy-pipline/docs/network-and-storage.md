# 网络与存储

## 网络

| Network | 用途 |
| --- | --- |
| `peach-devops` | GitLab、Jenkins、Nexus、Registry、可观测性内部通信 |
| `peach-cloud-runtime` | Middleware 与 Peach Cloud 业务通信 |

Jenkins 和 DevOps Nginx 同时加入两个网络；Prometheus 与 OTel Collector 同时加入两个网络。

## 受保护 Volume

| 数据 | Named Volume |
| --- | --- |
| Registry | `peach-registry-data` |
| GitLab config/data | `peach-gitlab-config` / `peach-gitlab-data` |
| Jenkins | `peach-jenkins-data` |
| Nexus | `peach-nexus-data` |
| MySQL | `peach-mysql-data` |
| Redis | `peach-redis-data` |
| Nacos | `peach-nacos-data` |
| MongoDB | `peach-mongo-data` |
| RocketMQ | `peach-rocketmq-store` |

所有 bind-mounted 日志统一放在 `${PEACH_LOG_ROOT}` 的 `devops/`、`middleware/`、`observability/`、`application/` 分类中。数据库文件、Jenkins Home、GitLab repository、Nexus blob 等核心数据必须使用 named volume。
