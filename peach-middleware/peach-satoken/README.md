# peach-satoken

`peach-satoken` 是 Peach Cloud 的 Sa-Token 统一 starter，用于沉淀各微服务中重复的 SaTokenDao、Session 序列化策略、Same-Token 校验以及网关鉴权配置。

该模块将普通 Servlet 微服务和 Gateway 网关拆开，避免 WebMvc、Reactive Gateway、Redis 依赖被隐式混用。

## 模块说明

```text
peach-satoken
  peach-satoken-core                    # 公共 SaTokenDao、Session 策略、配置属性
  peach-satoken-web-autoconfigure       # 普通 Servlet 服务自动配置
  peach-satoken-web-starter             # 普通 Servlet 服务引入入口
  peach-satoken-gateway-autoconfigure   # Gateway 响应式自动配置
  peach-satoken-gateway-starter         # Gateway 引入入口
```

业务模块只需要依赖 starter，不要直接依赖 autoconfigure 模块。

## 普通微服务接入

普通 Servlet 微服务使用：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-web-starter</artifactId>
</dependency>
```

引入后默认提供：

- 基于现有 `JedisConnectionFactory` 的 `SaTokenDao`
- Jackson 兼容的 `SaSession`
- 基于 `WebMvcConfigurer` 的 Same-Token 校验

服务仍然需要通过现有 Peach Redis 模块提供 Redis Bean，例如：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-common</artifactId>
</dependency>
```

或其他能够创建 `JedisConnectionFactory` 的 Peach Redis starter。

## 网关接入

Gateway 网关使用：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-gateway-starter</artifactId>
</dependency>
```

引入后默认提供：

- 基于现有 `JedisConnectionFactory` 的 `SaTokenDao`
- Jackson 兼容的 `SaSession`
- 用于外部登录 token 校验的 `SaReactorFilter`
- 向下游服务注入 Same-Token 的 `GlobalFilter`
- 与原网关实现兼容的 token 生成策略

网关 Redis 依赖仍由网关模块自己显式维护。如果网关同时需要 reactive Redis 和 blocking Redis，需要继续在 gateway POM 中保留对应依赖。

## 默认配置

多数服务不需要新增 `peach.satoken.*` 配置。

默认值如下：

```yaml
peach:
  satoken:
    dao:
      enabled: true
    session-strategy:
      enabled: true
    same-token:
      enabled: true
      log-path: true
    gateway:
      enabled: true
      inject-same-token: true
      token-strategy-enabled: true
      log-path: true
      white-list:
        - /login
        - /logout
        - /register
        - /getCaptcha
        - /checkCaptcha
        - /init
        - /doc.html
        - /swagger-resources
        - /webjars
        - /v3/api-docs
        - /v2/api-docs
        - /actuator
        - health
        - /sse
        - /favicon.ico
```

## 常用配置

关闭普通微服务的 Same-Token 校验：

```yaml
peach:
  satoken:
    same-token:
      enabled: false
```

关闭网关向下游注入 Same-Token：

```yaml
peach:
  satoken:
    gateway:
      inject-same-token: false
```

覆盖网关白名单：

```yaml
peach:
  satoken:
    gateway:
      white-list:
        - /login
        - /doc.html
        - /v3/api-docs
```

关闭 starter 提供的 Redis `SaTokenDao`：

```yaml
peach:
  satoken:
    dao:
      enabled: false
```

关闭 Session 策略重写：

```yaml
peach:
  satoken:
    session-strategy:
      enabled: false
```

关闭网关自定义 token 生成策略：

```yaml
peach:
  satoken:
    gateway:
      token-strategy-enabled: false
```

## 迁移步骤

普通 Servlet 微服务迁移：

1. 将 `sa-token-spring-boot-starter` 替换为 `peach-satoken-web-starter`。
2. 删除本地 `CustomSaTokenDao`。
3. 删除本地 `SaTokenConfigure`。
4. 删除本地 `SaTokenStrategyConfigure`。
5. 删除本地 `SaSessionForJacksonCustomized`。
6. 保留现有 Peach Redis 依赖。

Gateway 网关迁移：

1. 将 `sa-token-reactor-spring-boot-starter` 替换为 `peach-satoken-gateway-starter`。
2. 删除本地 `CustomSaTokenDao`。
3. 删除本地网关 `SaTokenConfigure`。
4. 删除本地网关 `SaTokenStrategyConfigure`。
5. 删除本地 `SaSessionForJacksonCustomized`。
6. 保留网关现有 reactive/blocking Redis 依赖。

## 注意事项

- 该 starter 不主动引入 `spring-boot-starter-data-redis`。
- 只有存在 `JedisConnectionFactory` 时，才会自动注入 `SaTokenDao`。
- 如果服务自己声明了 `SaTokenDao`，starter 不会覆盖。
- 普通微服务和网关使用不同 starter，不要在 gateway 中使用 `peach-satoken-web-starter`。
- 网关白名单使用 `contains` 方式匹配，配置时需要避免过短路径造成误放行。
