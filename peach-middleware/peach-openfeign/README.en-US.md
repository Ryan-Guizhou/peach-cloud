# peach-openfeign

English | [中文](README.md)

Last updated: 2026-08-12

artifactId: `peach-openfeign`

Runtime: Java 8, Spring Boot `2.7.13`, Spring Cloud `2021.0.5`, Spring Cloud Alibaba `2021.0.5.0`

## Role

`peach-openfeign` provides the OpenFeign starter and auto-configuration for Peach Cloud service-to-service HTTP calls.

It provides Same-Token propagation, RequestId propagation, OkHttp timeouts, explicit retry policy, Sentinel flow/degrade integration, unified exception mapping, and fallback validation.

It does not define business Feign contracts or generic header relay. Business clients belong to `*-openfeign-external` modules. Inbound Same-Token validation is handled by `peach-satoken-starter`.

## Modules

| Module | Responsibility |
| --- | --- |
| `peach-openfeign-autoconfigure` | Auto-configuration, properties, interceptors, retry, exceptions, Sentinel baseline rules, fallback validation |
| `peach-openfeign-starter` | Business dependency entry, aggregating autoconfigure, OkHttp, and Sentinel |

## Main Properties

| Property | Default | Description |
| --- | --- | --- |
| `peach.openfeign.enabled` | `true` | Enable auto-configuration |
| `peach.openfeign.same-token-enabled` | `true` | Inject Sa-Token Same-Token |
| `peach.openfeign.same-token-fail-fast` | `false` | Reject outbound call when Same-Token is missing; recommended `true` in shared production config |
| `peach.openfeign.request-id-enabled` | `true` | Inject RequestId |
| `peach.openfeign.upload-max-bytes` | `10485760` | Feign upload limit; large files should use direct object-storage upload |
| `peach.openfeign.retry.methods` | `GET,HEAD` | Retry-enabled HTTP methods; write methods must be explicit |
| `peach.openfeign.retry.statuses` | `429,503,504` | Retryable HTTP statuses |
| `peach.openfeign.retry.exceptions` | network/timeout exceptions | Retryable exception class names |
| `peach.openfeign.sentinel.enabled` | `true` | Enable Sentinel governance |
| `peach.openfeign.sentinel.flow-data-id` | `peach-openfeign-sentinel-flow-rules` | Nacos flow rule dataId |
| `peach.openfeign.sentinel.degrade-data-id` | `peach-openfeign-sentinel-degrade-rules` | Nacos degrade rule dataId |
| `peach.openfeign.fallback.validate-on-startup` | `true` | Validate `fallbackFactory` at startup |
| `peach.openfeign.fallback.fail-fast-if-missing` | `true` | Fail startup outside production profiles when fallback is missing |
| `feign.sentinel.enabled` | `true` | Enable Spring Cloud Alibaba Sentinel Feign integration |
| `feign.circuitbreaker.enabled` | `true` | Enable Spring Cloud OpenFeign circuit breaker integration |
| `feign.sentinel.rules` | current config | Feign client/method Sentinel circuit-breaker rules |

## Sentinel Rules

Nacos rule files:

| File | rule-type |
| --- | --- |
| `deploy/nacos/config/peach-openfeign-sentinel-flow-rules.json` | `flow` |
| `deploy/nacos/config/peach-openfeign-sentinel-degrade-rules.json` | `degrade` |

Resource names follow Feign `contextId`, for example `authFeignClient`, `fileFeignClient`, and `messageFeignClient`.

## Same-Token And RequestId

- Same-Token is resolved from the current HTTP request header first, then from `SaSameUtil.getToken()`.
- RequestId is only relayed from the current HTTP request header. The module does not create RequestId, use MDC, or keep a custom context holder.
- Non-Servlet, async, and scheduled Feign calls usually have no original RequestId. If `same-token-fail-fast=true` and Same-Token cannot be resolved, the outbound call fails before reaching the remote service.
- Inbound Same-Token validation belongs to the consumer service through `peach-satoken-starter`.
- Retryable HTTP statuses keep their original classification after retry exhaustion. For example, 429 is still handled as rate limiting, and 503/504 are still handled as service unavailable.

## Verification

```bash
node scripts/check-utf8.mjs
mvn -pl peach-middleware/peach-openfeign/peach-openfeign-autoconfigure,peach-setting/peach-setting-openfeign-external,peach-monitor/peach-monitor-openfeign-external -am -DskipTests compile -Pdevelopment
git diff --check
```
