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

**Status:** Implemented; covered by integration tests.

- [x] Refresh-complete event is the only event that publishes the runtime snapshot; EnvironmentChangeEvent only records that script reload is required.
- [x] Rules, trusted proxy networks, script transport settings, and execution limits are validated together before atomic publication.
- [x] Invalid configuration retains the last-known-good snapshot.
- [x] Tests cover pre-rebind event ordering, rapid consecutive changes, and enable/disable transitions.

### Task 2: Immutable runtime snapshot

**Status:** Implemented; covered by unit and integration tests.

- [x] Store validated maxMatchedRulesPerRequest, rules, trusted proxies, script configuration, allowlists, denylists, and failure mode in the immutable snapshot.
- [x] Request execution reads the captured snapshot rather than the mutable bound Properties object.
- [x] Request-pinned Lua source prevents an in-flight request from mixing script versions during a refresh.
- [x] Tests cover invalid matched-rule caps, last-known-good state, and in-flight script snapshot consistency.

### Task 3: Script readiness lifecycle

**Status:** Implemented; lifecycle tests included.

- [x] Readiness is updated for disabled startup, startup load, dynamic enablement, successful/failed reload, and superseded reload.
- [x] Successful publication is distinguished from a stale reload that was discarded.
- [x] Startup script download failure does not prevent Gateway startup; requests follow the configured failure mode.
- [x] Shutdown clears the readiness gauge and emits a lifecycle log.
- [x] Tests cover success, failure, superseded reload, dynamic enablement, and shutdown.

### Task 4: Remote script URL and network safety

**Status:** Implemented; adversarial validation tests included.

- [x] Require exact host allowlisting, HTTPS by default, explicit port allowlisting, and valid SHA-256 checksums for configured remote scripts.
- [x] Validate all DNS results and pin the HTTP connection to a validated address to reduce DNS-rebinding exposure.
- [x] Reject user-info, fragments, redirects, loopback, link-local, unspecified, multicast, reserved, unauthorized private, and cloud metadata destinations.
- [x] Private network access requires an explicit CIDR contained within RFC1918 or IPv6 ULA space.
- [x] Tests cover IPv4/IPv6 loopback, IPv4-mapped IPv6, link-local, unspecified, multicast, private CIDR, metadata, untrusted hosts, user-info, non-approved ports, redirects, size limits, timeouts, checksums, and invalid Lua.
- [x] Remote URL failure never silently falls back to local classpath scripts; the last-known-good script snapshot is retained.

### Task 5: Lua single-key contract

**Status:** Implemented; Redis integration verified.

- [x] Replace regex-only checks with a deliberately constrained Lua tokenizer and Redis command contract.
- [x] Reject KEYS[2], aliases, dynamic indexes, indirect Redis client access, non-whitelisted commands, and forbidden Lua runtime globals.
- [x] Verify all six bundled Lua scripts against the contract.
- [x] Test the six algorithms against Testcontainers Redis Standalone.

### Task 6: Infrastructure integration and recovery

**Status:** Partially verified; manual topology and full application E2E remain outstanding.

- [x] Nacos Testcontainers integration covers configuration delivery, valid refresh, invalid refresh, and last-known-good state.
- [x] Runtime refresh tests cover rapid updates and enable/disable transitions; enforcement tests cover in-flight request snapshot pinning.
- [x] Sa-Token tests use real login tokens to verify per-device Token Session isolation and tenant switching.
- [x] Two independent Gateway enforcement instances using separate Lettuce clients share one Redis quota.
- [x] Redis Cluster, Sentinel, and TLS smoke-test profiles and reproducible local Compose fixtures are provided.
- [x] OSS failure tests cover checksum mismatch, invalid Lua, oversized response, timeout, and preservation of the previous snapshot.
- [ ] Run Redis Cluster, Sentinel, and TLS profiles against the intended deployment topology.
- [ ] Run a full HTTP end-to-end test with two separately started Gateway application instances; the current CI test covers independent enforcement instances, not two deployed Gateway processes.

### Task 7: Load testing and observability

**Status:** CI concurrency test and operational tooling are present; target-environment soak remains manual.

- [x] The Redis integration test issues 10,000 concurrent Lua operations across two independent Redis clients and verifies exact quota accounting.
- [x] CI records achieved throughput and p95/p99 client latency; the test does not claim 10,000 RPS.
- [x] k6 examples cover 1,000, 5,000, and 10,000 VUs, hot and distributed keys, token pools, and a 30-minute soak.
- [x] Micrometer metrics use bounded scope/algorithm/outcome tags; Prometheus alerts and a Grafana dashboard are supplied.
- [ ] Run the k6 profiles and 30-minute soak against the target Gateway/Redis deployment and archive raw results.
- [ ] Collect target-environment Redis resource metrics and Netty EventLoop latency alongside HTTP latency percentiles.

### Task 8: Operations documentation and final verification

**Status:** Documentation and seed configuration are in place; final batch CI is pending.

- [x] Update Chinese and English README and rate-limit operations documentation.
- [x] Document script precedence, checksum requirements, SSRF policy, single-key contract, readiness, failure modes, rollout, rollback, and failure drills.
- [x] Both Nacos seed files are byte-identical and keep rate limiting disabled by default.
- [ ] Run the complete required CI after the current batched changes.
- [ ] Perform the final whole-branch review and close all Critical/Important findings.
- [ ] Obtain the required review and merge the single PR only after all required checks pass.

## Delivery

- Branch: `feature/gateway-rate-limit-production-hardening`
- Issue: #14
- PR title: `hardening(gateway): complete rate-limit production reliability`
