# peach-middleware

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-middleware`  
类型：中间件聚合模块

## 模块定位

`peach-middleware` 聚合 Redis、Redisson、Mongo、OpenFeign、Sa-Token、RocketMQ、Kafka 等中间件接入模块。它把常用中间件能力封装为 starter / autoconfigure，减少业务模块重复配置和重复样板代码。

本模块解决：

- 中间件自动配置和 starter 依赖聚合。
- 中间件通用工具、注解、模板类和 SPI。
- 业务接入中间件时的统一约定和示例。

本模块不解决：

- 中间件服务端部署和运维。
- 生产 Topic、队列、Redis 集群、Mongo 集群、认证中心等资源治理。
- 业务最终一致性、幂等和补偿的完整业务方案。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-redis` | Redis 工具、多级缓存、Redis Stream |
| `peach-redission` | Redisson 分布式锁、延迟队列、布隆过滤器、防重复 |
| `peach-mongo` | Mongo 自动配置和通用服务接口 |
| `peach-openfeign` | OpenFeign 自动配置和调用增强 |
| `peach-satoken` | Sa-Token Web / Gateway 集成 |
| `peach-rocket` | RocketMQ 事件、生产消费、事务消息、Outbox、幂等 |
| `peach-kafka` | Kafka 相关模块 |

## 通用接入方式

业务模块优先引入对应 starter：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
```

不要绕过 starter 直接依赖 autoconfigure，除非正在开发中间件封装本身。

## 分层约定

| 层级 | 说明 |
| --- | --- |
| `*-autoconfigure` | 核心 API、配置绑定、自动配置、默认实现、SPI |
| `*-starter` | 对业务暴露的依赖入口 |
| `*-common` | 同一中间件家族共享基础能力 |
| `*-example` | 可运行示例和生产覆盖方式样例 |

## 运行机制

1. 业务模块引入某个中间件 starter。
2. Spring Boot 自动加载 autoconfigure。
3. 自动配置按配置项、类路径、Bean 条件创建默认实现。
4. 业务通过注解、模板类或 Manager 类使用中间件能力。
5. 生产环境通过自定义 Bean、SPI、外部配置和平台资源治理覆盖默认行为。

## 边界与限制

- starter 只封装客户端侧接入，不部署 Redis、RocketMQ、Mongo、Kafka 等服务端。
- 默认内存实现、自动创建资源、测试配置不能直接作为生产语义。
- MQ、锁、缓存、延迟队列等能力都需要业务侧定义幂等、超时、重试和补偿策略。
- 中间件账号密码、连接串、Topic、Consumer Group 等生产配置不能写死在仓库。

## 构建与验证

```bash
mvn -f "peach-middleware/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware -am clean package -DskipTests -Pdevelopment
```

单模块验证示例：

```bash
mvn -pl peach-middleware/peach-redis -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| starter 不生效 | 依赖是否引入；自动配置条件是否满足 | 查看依赖树和 Spring Boot 条件报告 |
| 连接中间件失败 | 地址、端口、账号、网络、服务端状态 | 先用中间件原生命令或控制台验证连接 |
| 配置未生效 | 配置前缀、profile、Nacos 数据是否正确 | 对照子模块 README 和配置类 |
| 重试导致重复处理 | 幂等键和业务唯一性是否稳定 | 明确幂等存储和重复调用处理策略 |
| 生产资源被误创建 | 是否开启自动创建 Topic、队列、缓存结构 | 生产关闭自动创建，由平台侧预先治理 |
