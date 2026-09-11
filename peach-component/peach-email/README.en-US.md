# peach-email

English | [中文](README.md)

`peach-email` standardizes mail models, SMTP transport, provider routing, templates, retry and idempotency extensions. Business modules integrate through `peach-email-starter`.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-email-autoconfigure` | Public APIs, SMTP/template/retry/idempotency implementations and auto-configuration |
| `peach-email-starter` | Business integration dependency entry point |
| `peach-email-quickstart` | Minimal bootstrap and provider-configuration verification |

```mermaid
flowchart LR
    App[Business Service] --> Starter[peach-email-starter]
    Starter --> Auto[peach-email-autoconfigure]
    Quick[peach-email-quickstart] --> Starter
    Auto --> Send[EmailSendService]
    Send --> Provider[EmailTransport]
```

## Integration

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-email-starter</artifactId>
</dependency>
```

Supply provider credentials through environment variables, configuration services or secret management.

### QuickStart

The example lives in [`peach-email-quickstart`](./peach-email-quickstart/) and verifies minimal starter bootstrap and auto-configuration:

```bash
mvn -f peach-component/peach-email/peach-email-quickstart/pom.xml spring-boot:run -Pdevelopment
```

Continue to supply the SMTP provider, username and authorization code through local environment variables or configuration services. The QuickStart stores no real credentials.

## Boundaries

- SMTP passwords/tokens, full recipient lists, bodies and attachments must not enter logs.
- Automated sending requires a stable business idempotency key; an in-memory `IdempotencyStore` is not a production multi-instance persistence guarantee.
- Retry only recoverable failures; do not blindly retry authentication, invalid addresses or invalid parameters.
- Provider failover must account for duplicate-send risk.
