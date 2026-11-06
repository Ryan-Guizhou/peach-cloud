# Gateway Rate Limit Production Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Complete one production-hardening delivery for the existing Gateway rate-limit system.

**Architecture:** Trigger reconciliation after Spring Cloud refresh completes; validate and atomically publish immutable runtime state; harden remote Lua retrieval and script contract validation; verify with focused tests, real infrastructure, load tests, and operational docs.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Cloud Context, Spring Cloud Gateway WebFlux, Lettuce, Redis Lua, Nacos, Micrometer, JUnit 5, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-06-gateway-rate-limit-hardening-design.md`

## Global Constraints

- Preserve six existing algorithms and six scopes.
- Keep rate limiting disabled by default.
- Do not block Netty EventLoop for Redis or network operations.
- Keep Jedis Sa-Token DAO separate from reactive Lettuce rate-limit access.
- Remote script URL wins; local classpath is used only when no remote URL is configured.
- Remote script failures never silently fall back to local.
- Keep Redis Lua single-key Cluster semantics.
- Keep metric tags low-cardinality.
- One issue, one feature branch, one PR.

## Review Focus

- A failed Nacos refresh must not mix new properties with old rules.
- Rapid consecutive refresh events must not allow stale script loads to overwrite newer snapshots.
- IPv4-mapped IPv6, bracketed IPv6, DNS rebinding, and redirect destinations must not bypass remote-host restrictions.
- Lua aliases and dynamic indexing must not bypass the single-key script contract.
- Public login and non-tenant rules must retain existing semantics when tenant context is absent.

---

### Task 1: Deterministic post-rebind runtime refresh

**Files:**
- Modify: `RateLimitRuleRegistry.java`
- Modify: `TrustedProxyClientIpResolver.java`
- Modify: `RateLimitScriptLoader.java`
- Tests: corresponding registry, resolver, and loader tests

- [ ] Add tests proving refresh handlers are invoked from the refresh-complete event and ignore the pre-rebind EnvironmentChangeEvent.
- [ ] Run tests and confirm the new tests fail on current listener wiring.
- [ ] Switch runtime refresh handlers to Spring Cloud's refresh-complete event.
- [ ] Verify invalid config retains the last valid runtime state.
- [ ] Run focused tests.

### Task 2: Immutable runtime snapshot

**Files:**
- Modify: `RateLimitConfigurationSnapshot.java`
- Modify: `RateLimitRuleRegistry.java`
- Modify: `RateLimitEnforcementService.java`
- Tests: `RateLimitRuleRegistryTest.java`, `RateLimitEnforcementServiceTest.java`

- [ ] Add regression test: valid cap 32, then invalid cap 0; old rules and cap remain effective.
- [ ] Run test and verify it fails before implementation.
- [ ] Store validated maxMatchedRulesPerRequest in snapshot and make request execution read only snapshot state.
- [ ] Add snapshot generation/version if needed to correlate refresh diagnostics.
- [ ] Run focused tests.

### Task 3: Script readiness lifecycle

**Files:**
- Modify: `RateLimitScriptLoader.java`
- Modify: `RateLimitMetrics.java`
- Tests: script loader and metrics tests

- [ ] Test successful `Mono<Void>` reload updates Gauge to 1 and logs completion.
- [ ] Test failure preserves previous readiness and reports the correct state.
- [ ] Replace onNext-only success callback with completion-aware handling.
- [ ] Verify disabled startup and later dynamic enablement.
- [ ] Run focused tests.

### Task 4: Remote script URL and network safety

**Files:**
- Modify: `GatewayRateLimitProperties.java`
- Modify: `RateLimitScriptRepository.java`
- Tests: `RateLimitScriptRepositoryTest.java`

- [ ] Add tests for bracketed IPv6 link-local, loopback, RFC1918, unspecified, multicast, IPv4-mapped IPv6, metadata hosts, DNS-resolved private IP, untrusted host, user-info, non-approved port, and redirect response.
- [ ] Verify each adversarial test fails against the current implementation.
- [ ] Add explicit allowed-host and optional explicitly authorized internal CIDR settings with secure defaults.
- [ ] Validate all resolved addresses before connecting; disable redirects and avoid DNS re-resolution gaps where feasible.
- [ ] Keep test-only localhost server access through explicit test allowlisting.
- [ ] Run focused tests.

### Task 5: Lua single-key contract

**Files:**
- Modify: `RateLimitScriptRepository.java`
- Tests: `RateLimitScriptRepositoryTest.java`
- Resources: six bundled Lua scripts only if contract annotations or validation require it

- [ ] Add tests for direct KEYS[2], alias KEYS, dynamic index, helper function access, non-whitelisted Redis commands, and forbidden Lua libraries.
- [ ] Verify bypass examples fail current validation.
- [ ] Implement constrained validation/parser contract; do not claim general Lua static analysis if only a restricted grammar is supported.
- [ ] Verify all six bundled scripts satisfy the contract.
- [ ] Run focused tests and Testcontainers Redis integration tests.

### Task 6: Infrastructure integration tests

**Files:**
- Tests: Gateway/Nacos/Token Session/Redis integration test packages
- Test resources and CI workflow only where needed

- [ ] Add Nacos refresh lifecycle tests covering valid, invalid, rapid, enable/disable, and in-flight requests.
- [ ] Add two-token tenant isolation and tenant-switch tests.
- [ ] Add two Gateway application instances sharing a Redis quota.
- [ ] Add Redis Cluster/Sentinel/TLS verification profiles or reproducible compose scripts; report exactly which profiles ran in CI.
- [ ] Add OSS script failure/recovery tests.
- [ ] Run all relevant integration profiles.

### Task 7: Load testing and observability

**Files:**
- Create: reproducible load-test scripts/configuration and report template under `peach-gateway/docs/performance/`
- Modify: `RateLimitMetrics.java` and observability docs only where justified

- [ ] Add 1k/5k/10k concurrency profiles and sustained soak profile.
- [ ] Capture RPS, p50/p95/p99, status distribution, Redis resource metrics, Lua latency, and EventLoop latency.
- [ ] Cover global hot key, distributed keys, multiple rules, refresh, Redis degradation/recovery.
- [ ] Do not set pass thresholds without a measured baseline; document environment and raw results.
- [ ] Verify metric tags remain low-cardinality.

### Task 8: Operations docs and final verification

**Files:**
- Modify: Chinese/English Gateway README and rate-limit docs.
- Verify: `deploy/nacos/config/peach-gateway.yml` and `deploy-pipline/init/nacos/config/peach-gateway.yml`

- [ ] Document refresh ordering, script precedence, SHA-256, SSRF policy, Cluster contract, readiness, failure mode, rollout, rollback, and failure drills.
- [ ] Verify both Nacos seed files are byte-identical and rate limiting remains disabled.
- [ ] Run repository governance.
- [ ] Run affected Maven reactor verify including Testcontainers.
- [ ] Run load/infrastructure profiles and archive reports.
- [ ] Perform whole-branch code review; fix all Critical/Important findings.
- [ ] Create one PR linked to Issue #14; merge only after required checks and review pass.

## Delivery

- Branch: `feature/gateway-rate-limit-production-hardening`
- Issue: #14
- PR title: `hardening(gateway): complete rate-limit production reliability`
