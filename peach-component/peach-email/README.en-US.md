# peach-email

English | [中文](README.md)

Last updated: 2026-07-15
artifactId: `peach-email`
Type: email component aggregate module

## Purpose

`peach-email` provides a single integration point for email models, SMTP transport, provider routing, template rendering, retry, and idempotent sending. Business modules should depend on `peach-email-starter`; they must not directly depend on autoconfigure or maintain independent JavaMail sessions, retry loops, or provider-selection logic.

It does not provide persistent mail jobs, bulk marketing, bounce handling, delivery tracking, rate limiting, or an administration UI.

## Module Layout

```text
peach-component/peach-email
├── peach-email-autoconfigure
│   └── src/main
│       ├── java/com/peach/email
│       │   ├── autoconfigure  # EmailProperties, EmailAutoConfiguration
│       │   ├── core           # EmailMessage, EmailTransport, SendResult
│       │   ├── service        # EmailSendService
│       │   ├── router         # ProviderRouter
│       │   ├── smtp           # SMTP transport and connection provider
│       │   ├── template       # template resolution, rendering, and management
│       │   ├── retry          # RetryPolicy
│       │   └── Idempotency    # retained source package spelling
│       └── resources
│           ├── META-INF/services
│           └── templates
└── peach-email-starter
```

## Quick Start

Add the starter:

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-email-starter</artifactId>
</dependency>
```

`peach-email-autoconfigure` marks JavaMail, FreeMarker, and Commons Lang runtime dependencies as optional. An independent application must verify with `mvn dependency:tree` that its final dependency graph supplies them.

Configure a provider. The values below are placeholders; passwords and authorization codes must come from environment variables, a configuration center, or a secret manager.

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

Built-in provider names are `qq`, `163`, `gmail`, and `ali`. They use their built-in SMTP host, port `465`, and SSL by default. A custom provider without host or port falls back to `localhost:25`.

```java
EmailMessage message = EmailMessage.builder()
        .from("sender@example.com")
        .to(Collections.singletonList("receiver@example.com"))
        .subject("Order result")
        .text("Your order has been processed.")
        .idempotencyKey("order-mail:10001")
        .build();

SendResult result = emailSendService.sendAuto(message);
```

- `send(providerName, message)` sends through a named provider directly; it does not run the service idempotency, retry, or failover flow.
- `sendAuto(message)` checks idempotency, selects and sorts candidate providers, retries each provider, then fails over. The current implementation sorts again by priority after moving `default-provider`, so the default provider is not guaranteed to remain first.

## Configuration

Prefix: `peach.email`

| Property | Default | Description |
| --- | --- | --- |
| `default-provider` | none | Added to the `sendAuto` candidate ordering; final ordering still follows priority |
| `providers.<name>.username` | none | SMTP user name |
| `providers.<name>.password` | none | SMTP password or authorization code; never store it in source control |
| `providers.<name>.host` | built-in value | `localhost` for an unconfigured custom provider |
| `providers.<name>.port` | `465` for built-ins | `25` for an unconfigured custom provider |
| `providers.<name>.ssl` | `true` | When false, the SMTP implementation enables STARTTLS |
| `providers.<name>.priority` | `100` | Lower values have higher priority |
| `retry.max-attempts` | `3` | Maximum attempts for each candidate provider |
| `retry.base-delay-millis` | `200` | Base delay for exponential backoff, in milliseconds |

Configure both username and password. QQ, NetEase, and Gmail usernames are also checked against their expected email domains. The module does not validate that at least one provider exists, and built-in SPI providers may become automatic-send candidates even without credentials; production deployments should explicitly constrain the candidate set.

## Core Objects and Extension Points

| Object | Responsibility |
| --- | --- |
| `EmailMessage` | Immutable model supporting recipients, HTML, attachments, inline resources, headers, and an idempotency key |
| `EmailSendService` | Named-provider and automatic-routing send entry points |
| `ProviderRouter` | Discovers transports with JDK `ServiceLoader` and maintains provider contexts and priority |
| `EmailTransport` | Provider transport SPI |
| `SmtpConnectionProvider` | SMTP connection creation and lifecycle contract |
| `TemplateManager` | Combines Spring and SPI `TemplateResolver` implementations and delegates rendering |
| `RetryPolicy` | Retry count, retryable exception, and backoff strategy |
| `IdempotencyStore` | Stores successful-send idempotency records |

The main auto-configured beans use `@ConditionalOnMissingBean` and can be replaced from the application: `ProviderRouter`, `IdempotencyStore`, `RetryPolicy`, `SmtpConnectionProvider`, `TemplateRenderer`, and `TemplateManager`.

A new provider must implement `EmailTransport` and register it in `META-INF/services/com.peach.email.core.EmailTransport`. A new template source can be a `TemplateResolver` Spring bean or an implementation registered through its JDK SPI file. A custom `SmtpConnectionProvider` bean does not automatically update the static `SmtpConnections` entry used by the send path; complete that registration as well.

## Runtime Flow and Limits

```text
EmailMessage
    ├── send(provider) -> ProviderRouter -> EmailTransport -> SMTP
    └── sendAuto
          ├── IdempotencyStore.exists
          ├── default provider and priority ordering
          ├── RetryPolicy for one provider
          ├── provider failover
          └── IdempotencyStore.record after success
```

Template rendering is separate from `sendAuto`: render first, then put the result in `EmailMessage.text` or `EmailMessage.html`. `TemplateManager.resolve()` currently has an inverted null check, so a valid resolver result can still be reported as missing. Do not treat built-in templates as verified until this is fixed and tested.

- Credentials, full recipient lists, and mail bodies must never appear in logs, exceptions, documentation, or test fixtures.
- Use a stable business identifier plus notification type as the `sendAuto` idempotency key. Message-consumption idempotency and email-sending idempotency are separate concerns.
- Replace `SimpleIdempotencyStore` in production: it is an in-memory, no-TTL, single-JVM map that is lost on restart.
- Validate attachment file name, size, type, source, and read permission before entering the component. The three-argument `Attachment` path does not currently carry data into MIME construction, so it is not a verified attachment-sending capability.
- Retry sleeps on the caller thread. Keep SMTP timeouts and retry settings bounded, and move sending off the request path where appropriate.
- Transport failures are wrapped as ordinary `RuntimeException`, so the default retry logic cannot accurately distinguish authentication, parameter, and network failures.

## Build, Verification, and Troubleshooting

```bash
mvn -f "peach-component/peach-email/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

The module currently has no test sources. This validates compilation and packaging only, not SMTP, templates, attachments, retry, idempotency, or failover semantics.

| Symptom | Check | Resolution |
| --- | --- | --- |
| `EmailSendService` is missing | Starter dependency and auto-configuration imports | Check the dependency tree and auto-configuration conditions |
| Provider is not configured | Provider name matches `providers` key and SPI transport | Check name, host, port, and credentials |
| Username validation fails at startup | QQ, 163/126, or Gmail suffix | Correct the provider name or account |
| Authentication or TLS fails | Authorization code, SSL/STARTTLS, and port `465`/`587` | Verify the provider requirements with a test account |
| Automatic sending does not fail over | SPI registration and provider contexts | Check the SPI file and provider configuration |
| Template ID is missing | Resolver result and classpath template | Check `TemplateResolver` and template resources |
| Duplicate messages | Stable idempotency key and shared store | Replace the in-memory store with persistent shared storage |
| Send thread blocks too long | SMTP timeout and exponential backoff | Bound the caller timeout and move sending off the request path |
