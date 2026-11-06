# Gateway Rate Limit Production Hardening — Design

## Intent

Harden the already merged six-algorithm Gateway rate limiter so Nacos refreshes are coherent, Lua script loading is observable and safe, Redis topology claims are verifiable, and operators can safely roll out and diagnose the feature.

## Goals

- Refresh rules, trusted proxy networks, script transport settings, and runtime limits only after Spring Cloud configuration rebinding completes.
- Publish one validated immutable runtime snapshot; invalid refreshes must not partially affect request behavior.
- Make Lua readiness accurate for startup and asynchronous refresh.
- Restrict remote Lua retrieval to explicitly trusted destinations and avoid relying on regex as a Lua parser.
- Add reproducible integration and performance verification for supported deployment topologies.
- Keep current six algorithms, six scopes, request semantics, and disabled-by-default configuration.

## Non-goals

- No rewrite of the six existing rate algorithms.
- No Semaphore/concurrency limiter or AIMD/adaptive algorithm in this task.
- No unmeasured performance SLA claims.
- No default enablement of rate limiting.

## Approaches

1. **Patch listeners independently** — smallest code change, but leaves ordering and partial snapshot risks.
2. **Post-rebind event + immutable runtime snapshot** — use Spring Cloud's refresh-complete event and publish a validated snapshot consumed by request execution; recommended because it gives one coherent state boundary.
3. **Replace Nacos refresh with a custom configuration control plane** — strongest control but duplicates Spring Cloud/Nacos lifecycle and is excessive for the current project.

## Chosen design

Use Spring Cloud's `RefreshScopeRefreshedEvent` as the post-rebind trigger for runtime configuration reconciliation. Build the candidate configuration completely, validate it, and atomically publish it. Keep last-known-good state on any failure. Script reloads remain asynchronous and atomic; readiness is updated on completion, not on an emitted item from `Mono<Void>`.

For remote scripts, require explicit host allowlisting and safe IP resolution. Reject loopback, link-local, private, unspecified, multicast, and metadata destinations unless a specific internal destination is explicitly authorized. Disable redirects. Replace regex-only cluster validation with a deliberately constrained script contract that rejects aliases/dynamic KEYS access and non-whitelisted Redis calls; the six bundled scripts remain the canonical reference.

## Constraints

- Java 21, Spring Boot 3.5.x, Spring Cloud 2025.x, Spring Cloud Gateway WebFlux.
- Redis operations remain reactive Lettuce; Sa-Token's existing Jedis DAO remains unchanged.
- Lua scripts are external resources; OSS URL takes precedence, local classpath is used only when no remote URL is configured.
- Metrics must not tag IP, tenant, user, URI, or rule ID.
- Rate limiting remains disabled by default.
- One issue, one branch, one PR; merge only after CI and review.

## Acceptance

- Refresh completion is deterministic and tested.
- Rejected configuration leaves all last-known-good runtime state unchanged.
- Readiness gauge changes on successful/failed async reloads.
- Remote script destination validation and cluster script contract have adversarial tests.
- Standalone Redis integration tests pass; Cluster/Sentinel/TLS are either tested in CI or clearly covered by reproducible environment scripts.
- Load/soak tests produce measured reports without inventing thresholds.
- Bilingual docs and both Nacos seed configs remain consistent.
