# Gateway Rate Limit Production Implementation Plan

> Implementation is delivered as one pull request. The pull request is not merged by the implementation agent.

**Goal:** Complete the previously agreed four-stage Gateway multi-algorithm distributed rate-limiting capability in one implementation cycle.

**Architecture:** A single post-authentication Gateway GlobalFilter evaluates immutable Nacos rule snapshots. Rules execute sequentially through reactive Lettuce Redis and per-algorithm Lua scripts. Tenant identity comes from the current Sa-Token Token Session; client IP is derived only through explicitly trusted proxy chains.

**Tech Stack:** Java 21, Spring Boot 3.5.4, Spring Cloud Gateway WebFlux, Sa-Token 1.44.x, Spring Data Redis Lettuce, Redis Lua, Nacos, Micrometer, JUnit 5, Testcontainers.

## Global Constraints

- Do not block the Netty EventLoop with Redis operations.
- Do not trust client-supplied tenant headers or forwarding headers from untrusted peers.
- Keep the existing Jedis Sa-Token DAO; add a dedicated Lettuce reactive connection using the same peach.redis connection settings.
- Support all six algorithms: FIXED_WINDOW, SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER, TOKEN_BUCKET, LEAKY_BUCKET, and GCRA.
- Support all six scopes: GLOBAL, IP, URI, IP_URI, TENANT, and TENANT_URI.
- Remote OSS scripts take precedence. A local classpath script is used only when no remote URL is configured for that algorithm.
- A configured remote script must pass SHA-256 validation; remote failure must not silently fall back to a local script.
- Nacos updates publish validated immutable snapshots; invalid updates retain the previous valid snapshot.
- Metrics must not use IP, tenant, user, URI, or rule ID as tags.
- Create one PR only. Wait for all required CI checks. Do not merge main; the repository owner performs the merge.

## Four-Stage Scope

### Stage 1: Complete the rate-limit engine and request execution

- [x] Keep the six algorithm implementations and external Lua files from PR #11.
- [ ] Add a post-authentication Gateway GlobalFilter and prioritized rule execution.
- [ ] Add Global/IP/URI/IP_URI rule matching and stable Redis key generation.
- [ ] Add standard 429 response headers and short-circuit behavior.
- [ ] Ensure every Lua script uses a single Redis key for Redis Cluster compatibility.

### Stage 2: Tenant-aware and trusted client identity

- [ ] Write tenant/org/fiscal context into the current Token Session on login.
- [ ] Refresh the current Token Session when the user switches tenant context.
- [ ] Read tenant context from the current token only; never trust request headers.
- [ ] Offload blocking Sa-Token DAO Token Session reads to boundedElastic.
- [ ] Resolve X-Forwarded-For only when the TCP peer belongs to a configured trusted proxy CIDR.

### Stage 3: Nacos configuration, policy, failure handling, and metrics

- [ ] Atomically refresh rules, allowlists, and denylists from Nacos.
- [ ] Reload Lua scripts only when script settings or the enabled state changes.
- [ ] Support IP and tenant allowlists/denylists.
- [ ] Implement FAIL_OPEN and FAIL_CLOSED behavior.
- [ ] Publish low-cardinality Micrometer request and execution-latency metrics.
- [ ] Document algorithm selection, scope semantics, configuration, headers, and failure behavior in Chinese and English.

### Stage 4: Production verification and hotspot safety

- [ ] Test all six algorithms against a real Redis instance.
- [ ] Verify concurrent quota sharing across two independent Redis clients.
- [ ] Verify Redis connection failure and Lua execution failure behavior.
- [ ] Verify remote OSS precedence, checksum validation, atomic reload, and no fallback on remote failure.
- [ ] Verify trusted proxy chains, tenant-context isolation, allowlists, denylists, 429 headers, and both failure modes.
- [ ] Run repository governance checks and the affected Maven reactor verification.
- [ ] Open one PR and leave it unmerged for owner review.

## Review Focus

- A forged X-Forwarded-For header from an untrusted TCP peer must never change the IP quota identity.
- A tenant-scoped rule with a missing Token Session tenant must not silently bypass the rule.
- Multiple matching rules must short-circuit on the first rejection; prior Redis deductions are not rolled back.
- Remote script refresh failure must preserve the last complete script snapshot and must not silently load local scripts.
- Redis failure must produce the configured FAIL_OPEN or FAIL_CLOSED behavior without swallowing downstream route errors.

## Verification Commands

    node scripts/verify-changes.mjs
    mvn -pl peach-gateway/peach-gateway-core,peach-auth/peach-auth-service -am verify -Pdevelopment

Testcontainers integration tests require Docker.
