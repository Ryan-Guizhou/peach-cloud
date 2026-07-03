# peach-message

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-message`  
类型：消息业务域聚合模块

## 模块定位

`peach-message` 负责站内消息、公告、待办、未读状态和 WebSocket 推送等业务能力。它提供消息实体、领域服务、REST 接口、OpenFeign 外部接口和可启动服务。

本模块解决：

- 消息、公告、待办、未读状态等业务建模。
- 内部消息 REST API 和外部服务调用入口。
- WebSocket 推送相关业务接入。
- 消息服务独立启动和本地联调。

本模块不解决：

- RocketMQ、Kafka 等消息中间件 starter 的底层封装，相关能力在 `peach-middleware`。
- 生产级消息触达治理，例如短信、邮件、移动推送供应商集成。
- 所有推送场景的强送达保证。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-message-common` | 消息域公共对象和常量 |
| `peach-message-entity` | 消息、公告、待办等实体模型 |
| `peach-message-service` | 消息领域服务和数据访问 |
| `peach-message-rest` | 内部和外部 REST 接口 |
| `peach-message-openfeign-external` | 面向其他服务的 OpenFeign 接口 |
| `peach-message-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-message-launch/src/main/java/com/peach/message/launch/PeachMessageApplication.java` |
| 配置文件 | `peach-message-launch/src/main/resources/application-dev.yml` |
| 内部 REST | `peach-message-rest/src/main/java/com/peach/message/rest/internal/MessageController.java` |
| 外部 REST | `peach-message-rest/src/main/java/com/peach/message/rest/external/MessageExternalController.java` |
| 服务包 | `peach-message-service/src/main/java/com/peach/message/service` |

## REST 能力

| 控制器 | 路径前缀 | 说明 |
| --- | --- | --- |
| `MessageController` | `/message` | 消息业务接口 |
| `MessageExternalController` | `/message/external` | 跨服务消息调用入口 |

## 运行机制

1. `peach-message-launch` 启动服务并加载环境配置。
2. REST 层接收消息创建、查询、状态变更等请求。
3. Service 层处理消息、未读状态、公告或待办等业务逻辑。
4. 外部服务通过 `peach-message-openfeign-external` 或外部 REST 入口调用。
5. 需要实时推送时，结合 WebSocket 和认证上下文处理在线用户推送。

## 配置说明

- 启动配置位于 `peach-message-launch/src/main/resources/application-*.yml`。
- 数据库、Redis、Nacos、WebSocket 相关参数需要按环境确认。
- 若消息创建依赖 MQ 事件，生产环境需要结合 `peach-rocket` 或其他中间件模块设计幂等和重试。

## 边界与限制

- 站内消息落库不等于用户一定实时收到推送。
- WebSocket 在线状态、断线重连和多端同步需要业务侧明确策略。
- 外部 REST 和 OpenFeign 接口应由服务间认证和网关策略保护。
- 重复消息、重复待办、重复推送需要调用方或服务侧设计幂等键。

## 构建与验证

```bash
mvn -f "peach-message/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-message/peach-message-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-message/peach-message-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 消息接口不可访问 | 服务是否启动；路径前缀是否正确；网关是否配置路由 | 先直连服务，再经网关验证 |
| 未读数量异常 | 状态表、用户 ID、幂等处理是否正确 | 检查数据库记录和状态变更链路 |
| WebSocket 无推送 | 连接是否建立；用户上下文是否识别；服务日志是否报错 | 检查前端连接、认证和服务端推送逻辑 |
| Feign 调用失败 | Nacos 注册、服务名、调用方依赖是否正确 | 确认 `peach-message-openfeign-external` 和服务注册状态 |
| 重复消息 | 调用方是否重试；服务侧是否有幂等键 | 为业务消息定义稳定唯一键并在服务侧去重 |
