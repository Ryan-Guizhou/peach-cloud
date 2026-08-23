# peach-mongo

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-mongo`  
类型：Mongo 中间件聚合模块

## 模块定位

`peach-mongo` 提供 Mongo 自动配置和通用服务接口，帮助业务模块统一接入 MongoDB。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo 配置绑定、自动配置、通用接口 |
| `peach-mongo-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `PeachMongoProperties` | 绑定 `peach.mongo` 配置 |
| `MongoAutoConfigure` | Mongo 自动配置入口 |
| `IMongoService<T>` | Mongo 通用服务接口 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-mongo-starter</artifactId>
</dependency>
```

Mongo 连接参数需要以 `PeachMongoProperties` 和当前环境配置为准，不要在文档中写入真实生产连接串。

## 边界与限制

- 本模块不部署 MongoDB。
- 通用接口不替代复杂聚合查询和业务索引设计。
- 生产环境需要单独治理连接池、索引、慢查询、备份和权限。
- Mongo 文档结构变更需要兼容历史数据。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-mongo/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-mongo -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Mongo 连接失败 | URI、账号、密码、库名、网络 | 使用 Mongo 客户端验证连接 |
| Bean 未注入 | 是否引入 starter；自动配置条件是否满足 | 检查依赖和条件报告 |
| 查询性能差 | 索引和查询条件是否匹配 | 增加索引并限制扫描范围 |
| 写入结构不兼容 | 文档字段变更是否兼容旧数据 | 增加迁移或兼容读取逻辑 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
