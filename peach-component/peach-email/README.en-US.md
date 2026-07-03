# peach-email

English | [中文](README.md)

## Purpose

`peach-email` is an email starter that wraps SMTP connections, provider routing, template rendering, retry policy, and idempotent sending.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-email-autoconfigure` | Auto-configuration, core sending service, and default implementations |
| `peach-email-starter` | Starter exposed to business modules |

## Core Objects

- `EmailProperties`: binds `peach.email.*`.
- `EmailSendService`: email sending entrypoint.
- `EmailTransport`: transport abstraction.
- `ProviderRouter`: provider routing.
- `TemplateManager` / `TemplateRenderer` / `TemplateResolver`: template handling.
- `RetryPolicy`: retry strategy.
- `IdempotencyStore`: idempotency abstraction.

## Configuration Example

```yaml
peach:
  email:
    default-provider: qq
    providers:
      qq:
        username: ${MAIL_USERNAME}
        password: ${MAIL_PASSWORD}
        host: smtp.qq.com
        port: 465
        ssl: true
        priority: 1
    retry:
      max-attempts: 3
      base-delay-millis: 200
```

## Usage Notes

- Business modules should import `peach-email-starter`, not autoconfigure directly.
- Mail passwords or authorization codes must come from environment variables or a config center.
- Use `priority` and `default-provider` to define routing in multi-provider scenarios.
- Production environments should replace default in-memory idempotency with Redis/JDBC or another persistent store.

## Verification

```bash
mvn -f "peach-component/peach-email/pom.xml" -DskipTests package
```
