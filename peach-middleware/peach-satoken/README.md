# peach-satoken

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-satoken`  
类型：Sa-Token 中间件聚合模块

## 模块定位

`peach-satoken` 提供 Sa-Token 在 Web 服务和 Gateway 场景下的统一封装，包括核心配置、DAO、Session 策略和不同运行时的自动配置。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-satoken-core` | Sa-Token 核心配置、DAO、Session 策略 |
| `peach-satoken-web-autoconfigure` | Web 服务自动配置 |
| `peach-satoken-web-starter` | Web 服务 starter |
| `peach-satoken-gateway-autoconfigure` | Gateway 自动配置 |
| `peach-satoken-gateway-starter` | Gateway starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `PeachSaTokenProperties` | 绑定 `peach.satoken` 配置 |
| `PeachSaTokenDaoAutoConfiguration` | Sa-Token DAO 自动配置 |
| `PeachSaTokenSessionStrategyAutoConfiguration` | Session 策略自动配置 |
| `PeachSaTokenWebAutoConfiguration` | Web 场景自动配置 |
| `PeachSaTokenGatewayAutoConfiguration` | Gateway 场景自动配置 |

## 接入方式

Web 服务：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-web-starter</artifactId>
</dependency>
```

Gateway：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-gateway-starter</artifactId>
</dependency>
```

## 边界与限制

- Web 和 Gateway 场景 starter 不应混用。
- 会话持久化、Token 生命周期、踢人、续期等语义需要结合 Sa-Token 配置确认。
- Redis 序列化方式必须在多服务间保持一致。
- 网关侧放行不等于服务侧一定安全，关键接口仍应服务侧校验。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-satoken/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-satoken -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Token 校验失败 | Token 名称、过期时间、Redis、密钥是否一致 | 对照 `peach.satoken` 和 Sa-Token 配置 |
| Gateway 放行异常 | 白名单、路由、过滤器顺序 | 检查 Gateway 自动配置和路由 |
| Session 反序列化失败 | 多服务序列化配置是否一致 | 统一 DAO 和序列化策略 |
| Web 服务 Bean 冲突 | 是否同时引入 web/gateway starter | 按运行时只保留一个 starter |
