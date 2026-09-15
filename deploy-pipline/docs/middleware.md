# Middleware

Middleware 必须在 Jenkins 应用流水线之前就绪。Jenkins 只做连通性与健康状态验证，不负责启动、重建或初始化这些中间件。

| Service | Container | Runtime address | Persistent data | Verification |
| --- | --- | --- | --- | --- |
| MySQL | `peach-mysql` | `mysql:3306` | `peach-mysql-data` | authenticated `mysqladmin ping` |
| Redis | `peach-redis` | `redis:6379` | `peach-redis-data` | authenticated `PING` |
| Nacos | `peach-nacos` | `nacos:8848` | `peach-nacos-data` | `/nacos/actuator/health` |
| MongoDB | `peach-mongo` | `mongodb:27017` | `peach-mongo-data` | authenticated `db.runCommand({ ping: 1 })` |
| RocketMQ NameServer | `peach-rocketmq-namesrv` | `rocketmq-namesrv:9876` | - | Broker registration check |
| RocketMQ Broker | `peach-rocketmq-broker` | broker via NameServer | `peach-rocketmq-store` | `mqadmin clusterList` |

## MongoDB

MongoDB root 账号仅用于 Bootstrap 初始化和运维。`scripts/init/init-mongodb.sh` 会幂等创建业务账号，不存在时创建，已存在时保留；业务服务不得使用 root 账号。

Peach Mongo Starter 的真实配置前缀是 `peach.mongo`，Docker 环境通过 Spring Boot relaxed binding 使用：

```dotenv
PEACH_MONGO_URI=mongodb://<app-user>:<app-password>@mongodb:27017/<database>?authSource=<database>
MONGO_DATABASE=peach_cloud
```

Application Compose 将它映射为 `PEACH_MONGO_URI` 和 `PEACH_MONGO_DATABASE`。真实 URI 和密码只存在于私有 `deploy.env` / Jenkins Secret file，不在 Compose 或源码中提供可运行的默认密码。

MongoDB 数据文件保存在 `peach-mongo-data` named volume。Mongo 容器日志保持 Docker 标准输出，可通过 `docker logs peach-mongo` 查看；不额外挂载数据库日志文件目录，从而避免不同宿主环境的文件 UID/GID 权限影响 Mongo 启动。

## 数据保护

Middleware 的已有 named volume 均视为受保护数据。Bootstrap 只创建缺失资源并启动已停止容器，不执行 `docker compose down -v`、`docker volume prune`、`docker volume rm` 或数据库 destructive reset。
