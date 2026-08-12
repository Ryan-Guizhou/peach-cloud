# peach-satoken

[English](README.en-US.md) | 中文

最后更新：2026-08-12

`peach-satoken` 是 Peach Cloud 业务服务侧的 Sa-Token 接入模块，面向 Servlet 业务服务提供 Sa-Token Redis DAO、Session 序列化策略、Same-Token 校验、请求 ID 过滤器和当前用户上下文恢复能力。

Gateway 不依赖本模块。Gateway 在 `peach-gateway-core` 中独立维护 Reactor 场景下的 Sa-Token DAO、Session 策略、token 策略和过滤器。

## 模块定位

提供能力：

- 业务服务侧 Sa-Token Redis DAO。
- 业务服务侧 Jackson Session 序列化策略。
- 业务服务侧 Same-Token 校验。
- Servlet 请求 ID 过滤器，统一使用 `X-Request-Id`。
- Servlet 当前用户上下文恢复过滤器，将 Redis 中的用户上下文恢复到 `SecurityContextHolder`。
- method + path 形式的公开端点白名单，Same-Token 和用户上下文过滤器共用。

不提供能力：

- 用户、角色、菜单、权限数据维护。
- 登录接口、token 发放和用户上下文缓存写入。
- Gateway Reactor 场景能力。
- 绕过 Gateway 的完整服务端安全治理。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-satoken-autoconfigure` | 公共 API、Redis DAO、Session 策略和 Servlet 自动配置 |
| `peach-satoken-starter` | 业务服务接入依赖，聚合 autoconfigure、Sa-Token Web 与 Redis |

## 快速接入

Servlet 业务服务引入：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-starter</artifactId>
</dependency>
```

运行配置由各服务导入：

```text
deploy/nacos/config/peach-satoken.yml
```

## 自动配置

自动配置入口：

```text
peach-satoken-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

| 自动配置类 | 职责 |
| --- | --- |
| `PeachSaTokenDaoAutoConfiguration` | 注册业务服务侧 Sa-Token Redis DAO |
| `PeachSaTokenSessionStrategyAutoConfiguration` | 覆盖业务服务侧 Sa-Token Session 创建策略 |
| `PeachSaTokenWebAutoConfiguration` | 注册 Same-Token 拦截器、请求 ID 过滤器、用户上下文组件和过滤器 |

## 核心 API

| 类型 | 用途 |
| --- | --- |
| `SecurityContextHolder.get()` | 获取当前请求的 `UserContext` |
| `SecurityContextHolder.set(UserContext)` | 绑定当前请求用户上下文 |
| `SecurityContextHolder.clear()` | 清理当前线程用户上下文 |
| `UserContextSupport` | 按 loginId 从 Redis Hash 读取当前用户缓存 |
| `UserContextFilter` | 在非公开端点恢复当前用户上下文 |
| `RequestIdFilter` | 传递或生成 `X-Request-Id` |

## Redis 用户上下文契约

`UserContextSupport` 只读取 Redis，不写入 Redis。认证服务或业务服务需要按以下契约写入：

```text
key: peach:security:user:profile:{loginId}
type: Redis Hash
```

Hash 字段定义在 `SatokenConstant`：

| 常量 | Redis Hash 字段 | 说明 |
| --- | --- | --- |
| `USER_PROFILE_FIELD_USER_ID` | `userId` | 用户 ID，必须与 Sa-Token loginId 一致 |
| `USER_PROFILE_FIELD_USER_CODE` | `userCode` | 用户编码 |
| `USER_PROFILE_FIELD_USER_NAME` | `userName` | 用户名称 |
| `USER_PROFILE_FIELD_TENANT_ID` | `tenantId` | 当前租户 ID |
| `USER_PROFILE_FIELD_TENANT_NAME` | `tenantName` | 当前租户名称 |
| `USER_PROFILE_FIELD_ORG_ID` | `orgId` | 当前组织 ID |
| `USER_PROFILE_FIELD_ORG_CODE` | `orgCode` | 当前组织编码 |
| `USER_PROFILE_FIELD_ORG_NAME` | `orgName` | 当前组织名称 |
| `USER_PROFILE_FIELD_FISCAL` | `fiscal` | 当前会计期间 |
| `USER_PROFILE_FIELD_LANG` | `lang` | 语言代码；当前登录写入侧尚未明确来源 |
| `USER_PROFILE_FIELD_CONTEXT_VERSION` | `contextVersion` | 上下文版本号 |

不要把密码、token、身份证号、密钥、完整 DTO 等敏感字段放入 `UserContext` 或日志。

## Servlet 执行链路

1. Gateway 对外部请求生成 `X-Request-Id`，对非公开端点校验登录态，并为已登录的非公开请求注入 Same-Token。
2. 业务服务 `RequestIdFilter` 优先沿用合法请求 ID；缺失或非法时生成新 ID，并写入响应头。
3. `PeachSaTokenWebAutoConfiguration` 注册 Same-Token MVC 拦截器。公开端点直接放行；非公开端点执行 `SaSameUtil.checkCurrentRequestToken()`。
4. `UserContextFilter` 对公开端点允许未登录放行；非公开端点使用 `StpUtil.getLoginIdDefaultNull()` 获取 loginId。
5. loginId 为空或 Redis 用户上下文缺失时返回 `401`；缓存存在且 userId 匹配时写入 `SecurityContextHolder`。
6. 请求结束后 `UserContextFilter` 清理 `SecurityContextHolder`，避免 Servlet 线程复用污染。

## 过滤器与拦截器顺序

| 顺序 | 组件 | 作用 |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE + 20` | `RequestIdFilter` | 生成或传递 `X-Request-Id` |
| MVC 拦截器 | Same-Token `SaInterceptor` | 公开端点跳过；非公开端点校验 Same-Token |
| `Ordered.HIGHEST_PRECEDENCE + 40` | `UserContextFilter` | 公开端点跳过；非公开端点恢复 `UserContext` |

Same-Token 和用户上下文过滤器共用 `peach.satoken.user-context.public-endpoints`。

## 配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.satoken.dao.enabled` | `true` | 注册业务服务侧 Sa-Token Redis DAO |
| `peach.satoken.session-strategy.enabled` | `true` | 使用 Jackson 兼容 Session 类型 |
| `peach.satoken.same-token.enabled` | `true` | 启用 Servlet Same-Token 校验 |
| `peach.satoken.same-token.log-path` | `true`，Nacos 示例为 `false` | 是否以 DEBUG 记录 Same-Token 路径 |
| `peach.satoken.same-token.exclude-path-patterns` | `/error` | MVC 拦截器排除路径 |
| `peach.satoken.request-id.enabled` | `true` | 启用请求 ID 过滤器 |
| `peach.satoken.request-id.header-name` | `X-Request-Id` | 与 Gateway/OpenFeign 一致的请求头 |
| `peach.satoken.user-context.enabled` | `true` | 启用 Redis 用户上下文恢复过滤器 |
| `peach.satoken.user-context.public-endpoints` | 源码默认列表 / Nacos 可覆盖 | 允许未登录访问且跳过 Same-Token 的公开端点 |

## 日志格式

业务侧 Sa-Token 日志统一使用英文、参数化日志。建议字段顺序：

```text
requestId={}, method={}, path={}, userId={}, reason={}
userId={}, method={}, path={}
```

日志禁止记录 request body、query、token、password、完整 DTO、Redis 密码和完整 Redis key。需要定位用户上下文缓存问题时，记录 userId 和字段名即可。

## 边界与限制

- 所有业务服务和 Gateway 必须连接同一套 Sa-Token Redis 数据，并保持 `sa-token.token-name` 等配置一致。
- Gateway 不依赖 `peach-satoken`，不加载 Servlet 过滤器，也不使用业务侧 `SecurityContextHolder`。
- `UserContextSupport` 只读取约定 Redis key，不负责写入、刷新 TTL 或删除。
- 用户资料、租户/机构切换后，缓存更新策略由认证服务或具体业务服务负责。
- `lang` 字段当前仅定义读取契约，登录写入侧尚未明确来源；需要从请求头、用户偏好或登录参数中明确后再写入。

## 构建与验证

```bash
mvn -pl peach-middleware/peach-satoken/peach-satoken-autoconfigure -am -DskipTests compile -Pdevelopment
node scripts/check-utf8.mjs
git diff --check -- peach-middleware/peach-satoken
```

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 登录成功后首次业务请求 `401` | `peach:security:user:profile:{loginId}` 是否存在 | 确认认证服务已写入约定 Redis Hash，且服务连接同一 Redis DB |
| Gateway 认为未登录 | token 请求头、Sa-Token Redis、`sa-token.token-name` | 确认登录响应 token 与客户端请求头一致 |
| 下游返回 Same-Token 错误 | Gateway 注入开关、业务服务 Same-Token 配置、公开端点配置 | 检查 `peach.gateway.satoken.inject-same-token` 与 `peach.satoken.same-token.enabled` |
| 下游没有 requestId | 请求头名称 | 统一使用 `X-Request-Id` |
| `UserContextSupport` 未注入 | Redis 自动配置和 starter 依赖 | 检查 `StringRedisTemplate` 是否创建，避免只引 autoconfigure |
| 公开接口被 Same-Token 拦截 | `peach.satoken.user-context.public-endpoints` | 确认业务服务真实 Servlet 路径是否已配置，尤其是 Gateway `StripPrefix` 后的路径 |
