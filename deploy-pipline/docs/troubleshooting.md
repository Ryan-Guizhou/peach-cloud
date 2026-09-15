# 排障

| 现象 | 检查 |
| --- | --- |
| Jenkins 无法访问 Nexus | Jenkins 是否加入 `peach-devops`；Nexus 容器状态 |
| Maven 401 | `MAVEN_NEXUS_USERNAME/PASSWORD` 与 Nexus server IDs |
| Maven 仍直接访问公网 | 生成 settings 是否包含 `<mirrorOf>*</mirrorOf>` |
| Registry push 失败 | `local-registry` 是否运行；Docker daemon 是否能访问 `localhost:5000` |
| MySQL 初始化未执行 | 已存在业务表时 baseline 会保护性跳过 |
| Nacos 配置没覆盖 | 默认 preserve；只有 `init-nacos.sh --sync` 会覆盖 |
| Mongo Authentication failed | root/app 用户密码、`authSource=peach_cloud`、`peach-mongo` health |
| RocketMQ 不可用 | NameServer/Broker 状态与 `rocketmq-namesrv:9876` DNS |
| 服务找不到中间件 | 服务是否加入 `peach-cloud-runtime` |
| 旧日志看不到 | 新日志根目录为 `deploy-pipline/runtime/logs`，旧文件不会自动迁移 |

先运行 `scripts/bootstrap/inspect-existing.sh` 和 `scripts/bootstrap/verify-infrastructure.sh`，不要通过删除 Volume 解决连接或配置问题。
