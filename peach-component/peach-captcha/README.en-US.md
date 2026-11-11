# peach-captcha

English | [中文](README.md)

`peach-captcha` provides captcha generation, caching, validation and throttling extension points. Business modules depend only on `peach-captcha-starter`.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-captcha-autoconfigure` | `CaptchaService`, cache/providers, configuration and auto-configuration |
| `peach-captcha-starter` | Business integration dependency entry point |
| `peach-captcha-quickstart` | Minimal runnable integration verification |

```mermaid
flowchart LR
    App[Business Service] --> Starter[peach-captcha-starter]
    Starter --> Auto[peach-captcha-autoconfigure]
    Quick[peach-captcha-quickstart] --> Starter
    Auto --> Service[CaptchaService]
    Service --> Cache[CaptchaCacheService]
```

## Integration

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-captcha-starter</artifactId>
</dependency>
```

**Dependency boundary:** `peach-captcha-starter` provides the shared API and Hutool required by the default captcha implementation; MEMORY mode needs no Redis. REDIS mode requires the application to add `peach-redis-starter` and configure a reachable Redis instance. A missing Redis Bean must not be treated as a working shared cache.

The public entry is the auto-configured `CaptchaService`:

- `get(CaptchaVO)`: generate a captcha
- `check(CaptchaVO)`: frontend first-pass validation
- `verification(CaptchaVO)`: backend second-pass validation; the token is single-use

Defaults are `peach.captcha.cache-type=MEMORY` and `peach.captcha.service-type=BLOCKPUZZLE`. Shared cache in a cluster requires `REDIS` plus your own Redis setup.

## Quick Start

The example lives in [`peach-captcha-quickstart`](./peach-captcha-quickstart/). It is non-web and only verifies minimal starter integration. Production modules must not depend on it.

| Item | Detail |
| --- | --- |
| Capability samples (inject `CaptchaService`) | **Generate**: `get` returns a token and image; **Pass**: `check` then `verification`; **Reject**: wrong answer, expired cache / missing token → `API_CAPTCHA_INVALID` |
| Runner | `CaptchaDemoRunner`; disable with `quickstart.captcha.demo.enabled=false` |
| Minimal config | `peach.captcha.cache-type=MEMORY`, `peach.captcha.service-type=TEXT`, `peach.captcha.req-frequency-limit-enable=0` |
| Prerequisites | No Redis |

```bash
mvn -pl peach-component/peach-captcha/peach-captcha-quickstart -am spring-boot:run
mvn -pl peach-component/peach-captcha/peach-captcha-quickstart -am test
```

**Context isolation and memory safety:** each Spring application context owns its own `CaptchaService`, `CaptchaCacheService`, frequency limiter, configuration, and immutable image snapshot. MEMORY defaults to a 1000-entry bounded cache (`peach.captcha.cache-number`, valid 1–1000000); capacity exhaustion fails explicitly rather than clearing valid challenge keys. Expiry is lazy and no private scheduler/cleaner is started. Values are capped at 64Ki characters and TTL at 604800 seconds. Redis mode binds `StringRedisTemplate` from the current context. Custom image directories load up to 32 images per directory with a 2MiB per-image limit; snapshots never overwrite those of other application contexts.

## Boundaries

- Captcha does not replace login risk control, account lockout or device identification.
- Cluster deployments require cache semantics shared across instances; in-memory cache is not a production shared-cache guarantee.
- Never log plaintext captcha values, cache credentials or user-sensitive data.
- Successful-validation deletion, failure counts and throttling follow current implementation/configuration, not historical README assumptions.
