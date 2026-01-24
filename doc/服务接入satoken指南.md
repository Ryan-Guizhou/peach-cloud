# Sa-Token 分布式服务间鉴权接入指南 (Same-Token 模式)

为确保微服务调用链（如 `Gateway` -> `Monitor` -> `User`）的安全，所有服务必须统一集成 Sa-Token 并开启 Same-Token 校验。任何新服务接入都需遵循此规范。

## 1. 核心原理
所有服务共享同一套 `Redis` 和 `Sa-Token` 配置。
*   **网关 (Gateway)**: 作为入口，校验用户 Token，生成并注入 `Same-Token` 到请求头。
*   **中间服务 (Monitor)**: 校验上游（网关）传来的 `Same-Token`；调用下游（User）时，**透传**或**重新生成** `Same-Token`。
*   **下游服务 (User)**: 校验上游传来的 `Same-Token`。

---

## 2. 新服务接入步骤 (Standard Operating Procedure)

任何新接入的微服务（假设名为 `peach-new-service`）必须执行以下 3 步：

### 第一步：引入依赖
在 `pom.xml` 中引入 Sa-Token 和 Redis 依赖。

```xml
<!-- Sa-Token 权限认证 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.34.0</version>
</dependency>
<!-- Sa-Token 整合 Redis (必须与网关版本一致) -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>1.34.0</version>
</dependency>
<!-- Redis 连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 第二步：添加配置 (Nacos)
在 Nacos 的 `application-dev.yml` (或共享配置) 中，确保包含以下配置。所有服务必须使用**相同的 Redis** 和 **Same-Token 密钥**（默认自动生成，也可手动指定）。

```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000
  check-same-token: true  # [关键] 开启内部服务调用鉴权
```

### 第三步：配置拦截器 (Java)
创建 `SaTokenConfigure.java`，注册拦截器以强制校验 `Same-Token`。

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 1. 校验 Same-Token：保证请求来自网关或可信服务
            SaSameUtil.checkCurrentRequestToken();
        })).addPathPatterns("/**");
    }
}
```

---

## 3. 服务间调用 (Feign) 处理

如果 `Monitor` 需要调用 `User`，它必须将 `Same-Token` 传递下去。

**方案 A：Feign 拦截器自动透传 (推荐)**
在公共模块（如 `peach-common`）中实现一个 Feign 拦截器，自动将当前请求的 `Same-Token` 添加到下游请求中。

```java
@Component
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 为 Feign 请求添加 Same-Token
        template.header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
    }
}
```

**方案 B：手动透传**
```java
// 手动获取 Token 并添加到 Header
String token = SaSameUtil.getToken();
HttpHeaders headers = new HttpHeaders();
headers.set(SaSameUtil.SAME_TOKEN, token);
// 发送 RestTemplate 请求...
```

## 4. 文档维护
我将在项目根目录创建一份 `接入文档.md`，详细记录上述步骤，方便后续开发者查阅。
