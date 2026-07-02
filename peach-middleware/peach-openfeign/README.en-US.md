# Peach OpenFeign Starter

[中文](README.md)

Last updated: 2026/7/2  
Maintainer: Mr Shu  
Baseline: `peach-cloud 1.0.0-SNAPSHOT`, JDK 8, Spring Boot 2.7.x, Spring Cloud OpenFeign

## Table of Contents

- [1. Overview](#1-overview)
- [2. Responsibility Boundary](#2-responsibility-boundary)
- [3. Module Structure](#3-module-structure)
- [4. File Responsibilities](#4-file-responsibilities)
- [5. Default Behavior](#5-default-behavior)
- [6. Quick Start](#6-quick-start)
- [7. Full Configuration Reference](#7-full-configuration-reference)
- [8. Key Implementation Notes](#8-key-implementation-notes)
- [9. Auto-Configuration Details](#9-auto-configuration-details)
- [10. Build and Verification](#10-build-and-verification)
- [11. Troubleshooting](#11-troubleshooting)
- [12. Current Limitations and Suggestions](#12-current-limitations-and-suggestions)

## 1. Overview

`peach-openfeign` is the shared OpenFeign starter for Peach Cloud. It standardizes common request-header handling for service-to-service Feign calls.

The module currently focuses on two things:

- Automatically injecting `Same-Token` into downstream Feign requests
- Relaying selected incoming HTTP headers to downstream services

The module uses a two-part structure:

- `peach-openfeign-autoconfigure`: auto-configuration, properties, and interceptor implementation
- `peach-openfeign-starter`: the starter dependency used by business modules

This module does not:

- scan concrete `@FeignClient` definitions
- manage fallback, retry, timeout, or logging policies
- replace service-side authentication logic
- handle Gateway-to-service `Same-Token` injection

## 2. Responsibility Boundary

### 2.1 What the Module Provides

- A shared Feign `RequestInterceptor`
- Skipping the inbound `Same-Token` during header relay
- Re-injecting the current valid `Same-Token` for downstream calls
- Switches for header relay and same-token injection
- A configurable exclusion list for headers that must not be relayed
- Full relay for multi-value request headers

### 2.2 What the Module Does Not Provide

- Guaranteed Servlet request context in async or scheduled threads
- Ordering coordination across all custom Feign interceptors
- Business header reconstruction for non-HTTP call chains
- Gateway routing or downstream path mapping

### 2.3 Relationship with `peach-satoken`

- `peach-satoken` handles Sa-Token authentication and Same-Token validation on Gateway and regular services
- `peach-openfeign` handles outbound Feign propagation from one service to another

Recommended mental model:

- external request entering the system: `peach-satoken`
- internal Feign request leaving a service: `peach-openfeign`

## 3. Module Structure

```text
peach-middleware/peach-openfeign/
├── pom.xml
├── README.md
├── README.en-US.md
├── peach-openfeign-autoconfigure/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/peach/openfeign/
│       │   ├── autoconfigure/
│       │   │   └── PeachOpenFeignAutoConfiguration.java
│       │   └── config/
│       │       └── PeachOpenFeignProperties.java
│       └── resources/
│           └── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── peach-openfeign-starter/
    └── pom.xml
```

## 4. File Responsibilities

### 4.1 Root-Level Files

| Path | Purpose |
| --- | --- |
| `peach-middleware/peach-openfeign/pom.xml` | Aggregator POM for the `autoconfigure` and `starter` modules |
| `peach-middleware/peach-openfeign/README.md` | Main Chinese documentation |
| `peach-middleware/peach-openfeign/README.en-US.md` | Main English documentation |

### 4.2 `peach-openfeign-autoconfigure`

| File | Purpose |
| --- | --- |
| `autoconfigure/PeachOpenFeignAutoConfiguration.java` | Registers the Feign interceptor and applies header relay plus same-token injection |
| `config/PeachOpenFeignProperties.java` | Property model for `peach.openfeign.*` |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot auto-configuration entry |

### 4.3 `peach-openfeign-starter`

| File | Purpose |
| --- | --- |
| `peach-openfeign-starter/pom.xml` | Public starter used by business modules |

## 5. Default Behavior

After the starter is introduced, the following behavior is enabled by default:

- register `peachOpenFeignRequestInterceptor`
- enable downstream `Same-Token` injection
- enable relay of current Servlet request headers
- skip inbound `Same-Token` during relay
- exclude headers that should not be forwarded:
    - `content-type`
    - `content-length`
    - `host`
    - `connection`
    - `keep-alive`
    - `proxy-connection`
    - `te`
    - `trailer`
    - `transfer-encoding`
    - `upgrade`
    - `accept-encoding`

## 6. Quick Start

### 6.1 Dependency

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-openfeign-starter</artifactId>
</dependency>
```

In most services, this starter is pulled indirectly through `*-openfeign-external` modules.

### 6.2 Minimal Configuration

Most services do not need extra configuration:

```yaml
peach:
  openfeign:
    enabled: true
```

### 6.3 Runtime Flow

When a service receives an HTTP request and then calls another service through Feign:

1. the interceptor reads `ServletRequestAttributes`
2. allowed headers are copied to the downstream Feign request
3. any existing `Same-Token` is skipped
4. the current valid `Same-Token` is injected again

## 7. Full Configuration Reference

| Property | Default | Description |
| --- | --- | --- |
| `peach.openfeign.enabled` | `true` | Enables auto-configuration |
| `peach.openfeign.same-token-enabled` | `true` | Injects `Same-Token` into downstream Feign requests |
| `peach.openfeign.relay-headers` | `true` | Relays current Servlet request headers |
| `peach.openfeign.exclude-headers` | see below | Case-insensitive header names that must not be relayed |

Default `exclude-headers`:

```yaml
peach:
  openfeign:
    exclude-headers:
      - content-type
      - content-length
      - host
      - connection
      - keep-alive
      - proxy-connection
      - te
      - trailer
      - transfer-encoding
      - upgrade
      - accept-encoding
```

## 8. Key Implementation Notes

### 8.1 `Same-Token` Strategy

The current implementation does not simply forward the inbound `Same-Token`.

It does this instead:

1. skip `Same-Token` during header relay
2. call `SaSameUtil.getToken()` and inject a fresh value

This keeps downstream validation aligned with the current service context.

### 8.2 Multi-Value Header Relay

The current version preserves full multi-value headers instead of taking only the first value.  
Headers such as `Accept` and `Accept-Language` are no longer truncated.

### 8.3 Behavior Without Servlet Context

If the current thread does not carry `ServletRequestAttributes`:

- no exception is thrown
- HTTP headers are not relayed
- `Same-Token` injection still runs when enabled

This is useful for some internal, test, or non-Web execution paths.

### 8.4 Why Hop-by-Hop Headers Are Excluded

Headers like `connection`, `transfer-encoding`, and `upgrade` are valid only for a single transport hop. Forwarding them to downstream services can cause conflicts or undefined behavior, so they are excluded by default.

## 9. Auto-Configuration Details

Auto-configuration entry:

```text
peach-openfeign-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Registered configuration:

```text
com.peach.openfeign.autoconfigure.PeachOpenFeignAutoConfiguration
```

Default bean:

| Bean Name | Type | Purpose |
| --- | --- | --- |
| `peachOpenFeignRequestInterceptor` | `RequestInterceptor` | Handles header relay and same-token injection |

Activation conditions:

- `RequestInterceptor` exists on the classpath
- `peach.openfeign.enabled=true`
- no bean named `peachOpenFeignRequestInterceptor` already exists

## 10. Build and Verification

Module-level build command:

```bash
mvn -f "peach-middleware/peach-openfeign/pom.xml" -DskipTests compile
```

This change should verify:

- `peach-openfeign-autoconfigure` compiles
- `peach-openfeign-starter` compiles
- multi-value header relay compiles cleanly
- the new default exclusion list compiles cleanly

## 11. Troubleshooting

| Symptom | Check |
| --- | --- |
| downstream `Same-Token` validation fails | confirm the service includes `peach-openfeign-starter` and `same-token-enabled=true` |
| language-related headers look incomplete downstream | check whether another interceptor overwrites them; this module now supports full multi-value relay |
| some headers must not be forwarded | add them to `exclude-headers` |
| downstream calls from non-Web threads miss business headers | expected behavior; supply explicit headers or build a custom context strategy |
| custom Feign interceptors conflict | inspect whether multiple interceptors append or overwrite the same headers |

## 12. Current Limitations and Suggestions

Current limitations:

- only Servlet request context is supported today
- header relay only covers inbound HTTP request context
- the module still has no dedicated automated test class

Suggestions:

- if async context propagation is needed later, extract a dedicated request-header context provider
- if WebFlux upstream context must be supported, do not keep relying only on `RequestContextHolder`
- if more shared Feign interceptors are introduced, define ordering and ownership clearly
