# peach-kafka

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-kafka`  
类型：Kafka 中间件模块

## 模块定位

`peach-kafka` 是 Kafka 相关能力的预留或封装模块。当前应以源码和 `pom.xml` 中实际存在的类、依赖和自动配置为准，不应在 README 中承诺尚未实现的生产能力。

## 当前边界

- 如果模块只包含 POM 或基础骨架，说明当前尚未形成稳定对外 API。
- 业务接入 Kafka 前，应确认是否已有明确的 starter、autoconfigure、配置类、生产者、消费者和示例。
- 在能力未稳定前，不建议业务模块依赖该模块作为生产消息接入入口。

## 建议补齐内容

后续完善 Kafka 能力时，README 应同步补充：

- 对外 starter artifactId。
- 配置前缀和最小配置示例。
- 生产者发送 API 和消费者声明方式。
- 消费组、Topic、重试、死信、幂等和事务边界。
- 构建命令、测试命令和排障表。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-kafka/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-kafka -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 找不到 Kafka API | 源码是否已有对外类和 starter | 不要假设能力已实现，先补源码和示例 |
| 构建失败 | POM 依赖和父模块是否正确 | 从根目录用 `-am` 构建 |
| 业务误用骨架模块 | 是否缺少配置类和文档 | 等能力稳定后再对业务开放 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
