# peach-satoken

[English](README.en-US.md) | 中文

最后更新时间：2026-08-11
类型：Sa-Token 中间件聚合模块

## 模块定位

`peach-satoken` 统一 Peach Cloud 的 Sa-Token Redis DAO、Session 序列化、Same-Token 校验、请求 ID 和当前用户上下文读取。Servlet 业务服务引入一个 starter 即可接入；响应式 Gateway 直接复用 autoconfigure 中与 Web 运行时无关的 DAO 和 Session 能力。

本模块不负责用户信息写入，也不维护用户、角色和权限数据。业务服务只按 Sa-Token loginId 从约定 Redis key 读取 `UserContext`。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-satoken-autoconfigure` | 公共 API、Redis DAO、Session 策略及 Servlet 自动配置 |
| `peach-satoken-starter` | 业务服务接入依赖，聚合 autoconfigure、Sa-Token Web 与 Redis |

## 快速接入

Servlet 业务服务只需引入：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-starter</artifactId>
</dependency>
```

Gateway 使用 `sa-token-reactor-spring-boot-starter`，并直接依赖：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-autoconfigure</artifactId>
</dependency>
```

运行配置由各服务导入 `deploy/nacos/config/peach-satoken.yml`。

## 核心 API

| 类型 | 用途 |
| --- | --- |
| `SecurityContextHolder.get()` | 获取当前请求的 `UserContext` |
| `UserContextSupport` | 按 loginId 从 Redis Hash 读取当前用户缓存，key 前缀为 `peach:security:user:profile:` |

缓存写入由业务侧自行负责。不要把密码、token、身份证号等敏感字段放入 `UserContext` 或日志。

## 运行链路

1. Gateway 生成可信 `X-Request-Id`，校验外部请求登录态，并向下游注入 Same-Token。
2. `RequestIdFilter` 沿用合法请求 ID，并写入响应头。
3. Same-Token 拦截器校验请求来自 Gateway 或受信服务。
4. `UserContextFilter` 对公开路径允许未登录放行；其他请求使用 `StpUtil.getLoginIdDefaultNull()` 取得 loginId。loginId 为空或约定 key 没有用户信息时返回 401。
5. 缓存存在时写入 `SecurityContextHolder`，请求结束后清理；缓存缺失时返回 `401`。

## 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.satoken.dao.enabled` | `true` | 注册共享 Sa-Token Redis DAO |
| `peach.satoken.session-strategy.enabled` | `true` | 使用共享 Jackson Session 类型 |
| `peach.satoken.same-token.enabled` | `true` | 启用 Servlet Same-Token 校验 |
| `peach.satoken.same-token.log-path` | `true` | 以 DEBUG 记录校验路径 |
| `peach.satoken.same-token.exclude-path-patterns` | `/error` | Same-Token 排除路径 |
| `peach.satoken.request-id.enabled` | `true` | 启用请求 ID 过滤器 |
| `peach.satoken.request-id.header-name` | `X-Request-Id` | 与 Gateway/OpenFeign 一致的请求头 |
| `peach.satoken.user-context.enabled` | `true` | 启用 Redis 用户上下文与恢复过滤器 |
| `peach.satoken.user-context.public-paths` | `/auth/login` 等公开路径 | 未登录时允许放行的路径；其他路径 loginId 为空直接返回 401 |

## 扩展与边界

- `UserContextSupport` 只读取约定 Redis key，不提供写入、刷新 TTL 或删除能力。
- 所有服务必须连接同一套 Sa-Token Redis 数据，并保持 `sa-token.token-name` 等配置一致。
- 用户资料变更后由业务侧自行决定如何更新或删除用户上下文缓存。
- Gateway 不加载 Servlet 过滤器，也不使用业务侧 `SecurityContextHolder`。

## 构建与验证

```bash
mvn -pl peach-middleware/peach-satoken/peach-satoken-autoconfigure -am test
mvn -pl peach-gateway/peach-gateway-core,peach-auth/peach-auth-rest -am test -DskipTests
node scripts/check-utf8.mjs
git diff --check
```

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 登录成功后首次请求 `401` | `peach:security:user:profile:{loginId}` 是否存在 | 确认业务侧已写入约定 key 且服务连接同一 Redis DB |
| Gateway 认为未登录 | token 请求头、Sa-Token Redis、`sa-token.token-name` | 确认登录响应 token 与客户端请求头一致 |
| 下游返回 Same-Token 错误 | Gateway 注入开关、服务 Same-Token 配置 | 检查 `peach.gateway.satoken.inject-same-token` 与共享配置 |
| 下游没有 requestId | 请求头名称 | 统一使用 `X-Request-Id` |
| `UserContextSupport` 未注入 | Redis 自动配置和 starter 依赖 | 检查 `StringRedisTemplate` 是否创建，避免只引 autoconfigure |
