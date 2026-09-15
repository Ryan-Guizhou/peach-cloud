# Middleware

Middleware 必须在 Jenkins 应用流水线之前就绪。

| Service | Container | Runtime address | Persistent data |
| --- | --- | --- | --- |
| MySQL | `peach-mysql` | `mysql:3306` | `peach-mysql-data` |
| Redis | `peach-redis` | `redis:6379` | `peach-redis-data` |
| Nacos | `peach-nacos` | `nacos:8848` | `peach-nacos-data` |
| MongoDB | `peach-mongo` | `mongodb:27017` | `peach-mongo-data` |
| RocketMQ NameServer | `peach-rocketmq-namesrv` | `rocketmq-namesrv:9876` | - |
| RocketMQ Broker | `peach-rocketmq-broker` | broker via NameServer | `peach-rocketmq-store` |

MongoDB root account 只用于初始化/运维；业务使用 `MONGO_APP_USERNAME/MONGO_APP_PASSWORD`。现有 `peach-mongo` Starter 的 `MONGO_URI/MONGO_DATABASE` 可直接指向该实例。
