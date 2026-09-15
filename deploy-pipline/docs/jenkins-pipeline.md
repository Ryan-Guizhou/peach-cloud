# Jenkins Pipeline

Jenkins 是应用持续交付器，不是基础设施管理器。

```mermaid
sequenceDiagram
  participant G as GitLab
  participant J as Jenkins
  participant N as Nexus
  participant R as Registry
  participant D as Docker Runtime
  G->>J: Push Webhook
  J->>D: Verify infrastructure
  J->>N: Resolve Maven dependencies
  J->>N: mvn clean deploy
  J->>R: Push Git-SHA images
  J->>D: Pull and update selected services
  J->>D: Verify container / actuator health
```

`DEPLOY_SERVICES` 仍支持按服务选择，`STOP_UNSELECTED_SERVICES=false` 默认不停止其他业务服务。镜像 Tag 为当前 Git 的 12 位短 SHA。Jenkins 不执行 MySQL/Mongo/Nacos 初始化，也不启动 Middleware。
