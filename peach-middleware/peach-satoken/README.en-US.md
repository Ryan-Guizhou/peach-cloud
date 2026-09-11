# peach-satoken

English | [中文](README.md)

`peach-satoken` packages Peach Cloud's Sa-Token Web and internal Same-Token integration. Business services use `peach-satoken-starter` as the shared authentication foundation.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-satoken-autoconfigure` | Sa-Token Web/Same-Token configuration and auto-configuration |
| `peach-satoken-starter` | Business integration dependency entry point |
| `peach-satoken-quickstart` | Minimal Web integration verification |

## QuickStart

The example lives in [`peach-satoken-quickstart`](./peach-satoken-quickstart/) and verifies minimal Web integration. Business login, permission data, and the upstream Same-Token source remain owned by real services; the QuickStart stores no production token or fixed secret.

```bash
mvn -f peach-middleware/peach-satoken/peach-satoken-quickstart/pom.xml spring-boot:run -Pdevelopment
```

## Boundaries

- Login and user role/organization/permission data remain owned by the authentication domain; the starter is not the permission-data source of truth.
- Same-Token constrains internal service-call identity and does not replace business API authorization.
- Tokens, Same-Token values, passwords and sensitive authentication payloads must not enter logs or examples.
- Gateway and business-service responsibilities remain separate: ingress authentication/coarse filtering and final business authorization cannot replace one another.
