# peach-mongo

[English](README.en-US.md) | 中文

`peach-mongo` 提供 MongoDB 自动配置和统一访问入口，业务通过 `peach-mongo-starter` 接入。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo 配置、自动装配和通用服务契约 |
| `peach-mongo-starter` | 业务接入依赖入口 |
| `peach-mongo-quickstart` | 最小连接与自动装配验证 |

```xml
<dependency><groupId>com.peach</groupId><artifactId>peach-mongo-starter</artifactId></dependency>
```

## QuickStart

示例代码位于 [`peach-mongo-quickstart`](./peach-mongo-quickstart/)，用于验证 starter 的最小启动和自动装配。MongoDB URI、账号和密码只通过本地配置或环境变量提供。

```bash
mvn -f peach-middleware/peach-mongo/peach-mongo-quickstart/pom.xml spring-boot:run -Pdevelopment
```

## 边界

- Starter 不部署 MongoDB。
- Mongo URI、账号和密码必须来自安全配置来源，不写入 README 或源码。
- 通用访问接口不替代业务聚合查询、索引和数据生命周期设计。
- 文档结构演进需要明确旧数据兼容和迁移策略。
