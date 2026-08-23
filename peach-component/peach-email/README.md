# peach-email

[English](README.en-US.md) | 中文

最后更新时间：2026-07-15
artifactId：`peach-email`
类型：邮件组件聚合模块

## 模块定位

`peach-email` 统一封装邮件模型、SMTP 传输、provider 路由、模板渲染、重试和幂等能力。业务模块应依赖 `peach-email-starter`，不要直接依赖 autoconfigure，也不要各自维护 JavaMail Session、重试和 provider 选择逻辑。

本模块不提供邮件任务持久化、批量营销、退信处理、投递追踪、限流和管理后台。

## 模块导航

```text
peach-component/peach-email
├── peach-email-autoconfigure
│   ├── src/main/java/com/peach/email
│   │   ├── autoconfigure  # EmailProperties、EmailAutoConfiguration
│   │   ├── core           # EmailMessage、EmailTransport、SendResult
│   │   ├── service        # EmailSendService
│   │   ├── router         # ProviderRouter
│   │   ├── smtp           # SMTP transport 与连接提供者
│   │   ├── template       # 模板解析、渲染与管理
│   │   ├── retry          # RetryPolicy
│   │   └── Idempotency    # IdempotencyStore（沿用当前源码包名）
│   └── src/main/resources
│       ├── META-INF/services  # EmailTransport、TemplateResolver SPI
│       └── templates           # 内置 FreeMarker 模板
└── peach-email-starter         # 业务接入依赖
```

## 快速接入

### 1. 引入 starter

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-email-starter</artifactId>
</dependency>
```

项目统一管理 `${revision}` 时无需重复声明版本。

当前 `peach-email-autoconfigure` 将 JavaMail、FreeMarker、Commons Lang 等运行时依赖标记为 optional。独立应用只引入 starter 时，必须通过 `mvn dependency:tree` 确认这些依赖已由应用显式提供。

### 2. 配置 provider

以下值仅为占位示例。密码或授权码必须来自环境变量、配置中心或密钥管理服务。

```yaml
peach:
  email:
    default-provider: qq
    providers:
      qq:
        username: ${PEACH_EMAIL_USERNAME}
        password: ${PEACH_EMAIL_PASSWORD}
        priority: 10
    retry:
      max-attempts: 3
      base-delay-millis: 200
```

内置 provider 名称为 `qq`、`163`、`gmail` 和 `ali`。它们默认使用各自 SMTP host、465 端口和 SSL；自定义 provider 未配置 host、port 时会回退到 `localhost:25`。

### 3. 发送邮件

```java
EmailMessage message = EmailMessage.builder()
        .from("sender@example.com")
        .to(Collections.singletonList("receiver@example.com"))
        .subject("订单处理结果")
        .text("订单已处理完成")
        .idempotencyKey("order-mail:10001")
        .build();

SendResult result = emailSendService.sendAuto(message);
```

- `send(providerName, message)`：指定 provider 直接发送，不执行 `EmailSendService` 的幂等、重试和故障转移。
- `sendAuto(message)`：先检查幂等键，再组织候选 provider 并按 priority 排序，逐个 provider 重试和故障转移。当前实现会在移动默认 provider 后再次按 priority 排序，因此 `default-provider` 不保证最终排在第一位。

## 配置说明

配置前缀：`peach.email`

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `default-provider` | 无 | 加入 `sendAuto` 候选排序；最终顺序仍由 priority 决定 |
| `providers.<name>.username` | 无 | SMTP 用户名 |
| `providers.<name>.password` | 无 | SMTP 密码或授权码，禁止明文入库 |
| `providers.<name>.host` | 内置 provider 使用预设值 | 自定义 provider 未配置时为 `localhost` |
| `providers.<name>.port` | 内置 provider 为 `465` | 自定义 provider 未配置时为 `25` |
| `providers.<name>.ssl` | `true` | `false` 时由 SMTP 实现启用 STARTTLS |
| `providers.<name>.priority` | `100` | 数值越小越优先 |
| `retry.max-attempts` | `3` | 每个候选 provider 的最大尝试次数 |
| `retry.base-delay-millis` | `200` | 指数退避基准毫秒数 |

配置 provider 时应同时提供 username 和 password。QQ、网易和 Gmail 的用户名还会执行邮箱域名校验。当前代码不会校验“至少配置一个 provider”，且内置 SPI provider 即使没有凭证也可能进入自动发送候选；生产接入必须显式限制候选集合。

## 核心对象

| 对象 | 职责 |
| --- | --- |
| `EmailMessage` | 不可变邮件模型，支持 to/cc/bcc、HTML、附件、内嵌资源、自定义 header 和幂等键 |
| `EmailSendService` | 指定发送与自动路由发送入口 |
| `ProviderRouter` | 通过 JDK `ServiceLoader` 发现 transport，维护 provider 上下文和优先级 |
| `EmailTransport` | provider 传输 SPI |
| `SmtpConnectionProvider` | SMTP 连接创建与生命周期接口 |
| `TemplateManager` | 聚合 Spring Bean 与 SPI `TemplateResolver`，委托 `TemplateRenderer` 渲染 |
| `RetryPolicy` | 重试次数、可重试异常与退避策略 |
| `IdempotencyStore` | 发送成功结果的幂等存储 |

## 扩展方式

自动配置的主要 Bean 均使用 `@ConditionalOnMissingBean`，可通过业务侧 `@Bean` 覆盖：

- `ProviderRouter`：自定义 provider 注册和路由规则。
- `IdempotencyStore`：使用 Redis 或数据库实现跨实例、可过期的幂等。
- `RetryPolicy`：区分认证失败、参数错误、网络异常等可重试条件。
- `SmtpConnectionProvider`：控制连接复用、回收和健康检查。当前发送链路通过静态 `SmtpConnections` 获取 provider；仅声明自定义 Bean 不会自动写入该静态入口，需要同步完成注册。
- `TemplateRenderer`：替换 FreeMarker 渲染。
- `TemplateManager`：自定义模板编排。

新增 provider 时实现 `EmailTransport`，并在
`META-INF/services/com.peach.email.core.EmailTransport` 中注册实现类。新增模板来源既可注册 `TemplateResolver` Bean，也可使用对应 JDK SPI 文件。

## 运行机制

```text
EmailMessage
    │
    ├── send(provider) ──> ProviderRouter ──> EmailTransport ──> SMTP
    │
    └── sendAuto
          ├── IdempotencyStore.exists
          ├── defaultProvider + priority 排序
          ├── RetryPolicy（单 provider 重试）
          ├── provider 故障转移
          └── 成功后 IdempotencyStore.record
```

模板渲染由 `TemplateManager` 独立完成；调用方将渲染结果设置到 `EmailMessage.text` 或 `EmailMessage.html` 后再发送。

当前 `TemplateManager.resolve()` 的空值判断方向存在缺陷，有效 resolver 结果可能最终仍报 `template id not found`。修复并补充测试前，不应把内置模板能力视为已验证可用。

## 生产边界

`REQUIRED`：

- 凭证只能从安全配置来源注入，日志、异常、README 和测试数据不得输出真实授权码。
- `sendAuto` 的幂等键应由稳定业务主键和通知类型组成。
- 消费消息后发邮件时，业务消费幂等与邮件发送幂等必须分别设计。
- 附件文件名、大小、类型和来源必须在进入组件前校验。

`PREFERRED`：

- 生产环境覆盖默认 `SimpleIdempotencyStore`。该实现仅保存在单 JVM 内存中，没有 TTL，重启后丢失且可能持续占用内存。
- 根据 SMTP provider 的限流规则配置连接、超时和重试，避免同步退避阻塞业务线程。
- 对发送成功率、耗时、provider 切换和最终失败建立脱敏指标。
- 使用附件前补充数据源、MIME 构建和大小限制测试；当前三参数 `Attachment` 构造路径没有携带实际内容，不能作为已完成的附件发送能力。

`LEGACY_COMPATIBLE`：

- `Idempotency` 包名以及 `GmailSmtpTtransport` 等现有拼写属于兼容事实；新增 API 不继续复制此命名风格。
- 默认重试会把 transport 返回的失败包装为普通 `RuntimeException`，不能准确区分认证、参数和网络故障。

`FORBIDDEN`：

- 将邮箱密码、授权码、完整收件人列表或邮件正文写入日志。
- 把内存幂等视为跨实例或持久化保证。
- 仅依赖重试保证不重复发送。
- 把默认线程级 SMTP transport 缓存描述为有容量、空闲回收和统一销毁的连接池。

## 构建与验证

```bash
mvn -f "peach-component/peach-email/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

当前模块没有测试源码；上述命令只能验证编译与打包，不能证明 SMTP、模板、附件、重试、幂等和故障转移行为正确。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `EmailSendService` 未注入 | 是否引入 starter；自动配置 imports 是否生效 | 检查依赖树和自动配置条件 |
| provider 未配置 | provider 名称是否与 `providers` key、SPI transport 名称一致 | 核对名称、host、port 和凭证 |
| 启动时用户名校验失败 | QQ、163/126、Gmail 地址后缀是否匹配 | 修正 provider 名称或账号 |
| 认证或 TLS 失败 | 授权码、SSL/STARTTLS、465/587 端口是否匹配 | 使用测试账号验证 provider 要求 |
| 自动发送没有故障转移 | 候选 transport 是否由 SPI 注册；上下文是否存在 | 检查 SPI 文件和 provider 配置 |
| 模板 ID 找不到 | resolver 是否返回有效路径；模板是否在 classpath | 检查 `TemplateResolver` 与模板资源 |
| 重复发送 | 是否使用稳定幂等键；是否仍为内存实现 | 替换为共享持久化 `IdempotencyStore` |
| 发送线程长时间阻塞 | SMTP 超时和指数退避是否过大 | 限制调用超时并将发送移出请求主链路 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
