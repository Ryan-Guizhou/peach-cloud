# peach-openfeign

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-openfeign`  
类型：OpenFeign 中间件聚合模块

## 模块定位

`peach-openfeign` 提供 OpenFeign 自动配置和 starter，统一服务间 HTTP 调用的基础行为。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-openfeign-autoconfigure` | OpenFeign 配置绑定和自动配置 |
| `peach-openfeign-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `PeachOpenfeignProperties` | 绑定 `peach.openfeign` 配置 |
| `PeachOpenfeignAutoConfiguration` | 自动配置入口 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-openfeign-starter</artifactId>
</dependency>
```

业务服务间调用应优先依赖对应业务模块的 `*-openfeign-external`，不要在调用方重复声明同一接口。

## 边界与限制

- 本模块只封装客户端侧调用配置，不提供服务发现中心。
- 超时、重试、降级和熔断策略需要按业务链路明确。
- OpenFeign 调用不应绕过服务间鉴权和审计策略。
- 文件上传、大响应体和长连接场景需要单独确认配置。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-openfeign/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-openfeign -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Feign Bean 未注入 | 是否启用 Feign 扫描；是否引入 starter 和 external 模块 | 检查启动类和依赖 |
| 调用超时 | 目标服务、网络、超时配置 | 先直连目标服务，再调整超时 |
| 404 或路径错误 | Feign 接口路径与服务 REST 路径是否一致 | 对照提供方 Controller |
| 重试放大流量 | 重试次数和幂等语义是否匹配 | 非幂等接口谨慎开启重试 |
