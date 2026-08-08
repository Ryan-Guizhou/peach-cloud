# peach-email 模块参考

本文只记录当前源码可验证的入口、默认值和边界。配置、API 或 SPI 变化后必须重新核对源码与 POM。

## 模块导航

```text
peach-component/peach-email
├── pom.xml
├── README.md
├── peach-email-autoconfigure
│   ├── pom.xml
│   └── src/main
│       ├── java/com/peach/email
│       │   ├── autoconfigure
│       │   │   ├── EmailProperties.java
│       │   │   └── EmailAutoConfiguration.java
│       │   ├── core
│       │   │   ├── EmailMessage.java
│       │   │   ├── EmailTransport.java
│       │   │   └── SendResult.java
│       │   ├── service/EmailSendService.java
│       │   ├── router/ProviderRouter.java
│       │   ├── smtp
│       │   ├── template
│       │   ├── retry
│       │   └── Idempotency
│       └── resources
│           ├── META-INF/services
│           └── templates
└── peach-email-starter
    └── pom.xml
```

导航时忽略 `target/` 和 `.flattened-pom.xml`。

## 可验证入口

- `EmailSendService.send(providerName, message)`：指定 provider 直接发送。
- `EmailSendService.sendAuto(message)`：幂等检查、provider 排序、单 provider 重试和故障转移。
- `EmailMessage`：不可变邮件模型，Builder 支持收件人、正文、附件、内嵌资源、header 和幂等键。
- `ProviderRouter`：用 `ServiceLoader<EmailTransport>` 发现 provider。
- `TemplateManager`：聚合 Spring Bean、SPI resolver 和默认 resolver，委托 renderer。
- 默认可覆盖 Bean：`ProviderRouter`、`EmailSendService`、`IdempotencyStore`、`RetryPolicy`、`SmtpConnectionProvider`、`TemplateRenderer`、`TemplateManager`。

## 配置事实

前缀为 `peach.email`：

| 字段 | 默认值/行为 |
| --- | --- |
| `default-provider` | 无 |
| `providers.<name>.host` | qq/163/gmail/ali 使用内置 host；其他为 localhost |
| `providers.<name>.port` | 内置 provider 为 465；其他为 25 |
| `providers.<name>.ssl` | true |
| `providers.<name>.priority` | Router 未设置时按 100 排序 |
| `retry.max-attempts` | 3 |
| `retry.base-delay-millis` | 200ms |

内置 SPI transport 名称为 `qq`、`163`、`gmail`、`ali`。新增 provider 必须同时实现 `EmailTransport` 并更新 `META-INF/services/com.peach.email.core.EmailTransport`。

## 默认实现边界

- `SimpleIdempotencyStore` 是无 TTL 的 JVM 内存 Map，不能跨实例，重启后丢失。
- `ThreadSmtpConnectionProvider` 的线程/连接生命周期必须在生产接入时复核。
- 重试使用当前线程 sleep；高延迟和高重试次数会阻塞调用线程。
- 模板渲染不在 `sendAuto` 内自动发生。
- provider 凭证校验允许 username/password 缺失作为占位，但实际发送仍会失败。
- `TemplateManager.resolve()` 当前空值判断方向错误，有效模板路径可能仍被判为不存在。
- `default-provider` 移到候选首位后又执行 priority 排序，不保证最终优先。
- 内置 SPI provider 未配置凭证时仍有上下文，可能进入 `sendAuto` 候选。
- transport 失败被包装为普通 `RuntimeException`，默认重试不能准确区分故障类型。
- 自定义 `SmtpConnectionProvider` Bean 不会自动更新静态 `SmtpConnections`。
- 三参数 `Attachment` 不携带实际数据源，当前 MIME 构建会跳过该附件路径。
- autoconfigure 的邮件、模板等运行时依赖为 optional；最终应用必须检查依赖树。
- 当前模块没有测试源码，构建通过不等于发送语义已验证。

## 变更检查表

- 新配置字段：Properties、metadata、README、测试同步。
- 新 provider：接口实现、SPI 文件、默认 host/port（如需要）、凭证校验、测试同步。
- 新模板来源：resolver、SPI 或 Bean、资源路径、变量安全、测试同步。
- 可靠性调整：明确幂等写入时机、重试分类、故障转移重复风险和中断处理。
- 公共 API 改名：先做影响分析，保留兼容层或说明迁移。

## 验证

```bash
mvn -f "peach-component/peach-email/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```
