# peach-common

English | [中文](README.md)

Last Updated: 2026-09-08
artifactId: `peach-common`
Type: Shared Common Module

## Module Scope

`peach-common` is the shared foundation dependency for backend services. It provides stable contracts and utility capabilities without business domain semantics. Business, component, and middleware modules may depend on it, but `peach-common` must never reverse-depend on specific business domains, Web runtime, database access, Redis clients, Sa-Token implementations, or vendor SDKs.

This module provides:

- Unified response models, exception definitions, pagination, and base entity contracts.
- Cross-module reusable utilities for strings, collections, date/time, desensitization, ID generation, and symmetric encryption.
- Minimal read-only audit context contracts (`AuditContextProvider`), implemented by concrete security modules.
- SPI-based symmetric encryption and unique ID generation extension mechanisms.

This module does NOT provide:

- Domain-specific business processes, cache keys, MQ topics, table schemas, or domain status codes.
- Web controllers, global exception handlers, filters, interceptors, or Spring Boot auto-configuration.
- Runtime auto-wiring of Redis, MQ, object storage, or Sa-Token.
- Hardcoded production secrets, tokens, private keys, or credentials.

## Module Structure

```text
peach-common
├── pom.xml
├── README.md
├── README.en-US.md
└── src
    ├── main
    │   ├── java/com/peach/common
    │   │   ├── audit/            # Audit context contracts and entity filler
    │   │   ├── constant/         # Cross-module shared constants
    │   │   ├── exception/        # Business and validation exceptions
    │   │   ├── id/               # ID generation SPI, providers, and implementations
    │   │   ├── response/         # Response models and status codes
    │   │   └── util/             # String, date, encrypt, desensitize, and random tools
    │   └── resources
    │       ├── default/          # Fallback encryption keys (development only)
    │       └── META-INF/services # Java SPI registration files
    └── test
        └── java/com/peach/common
```

`target/`, `.flattened-pom.xml`, and IDE/dependency caches are not part of the source layout.

## Maven Integration

Include the dependency in business modules:

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-common</artifactId>
</dependency>
```

The version is centrally managed by the root POM `dependencyManagement`, so explicit version tags are typically unnecessary.

## Core Capabilities

| Package Path | Responsibility |
| --- | --- |
| `com.peach.common.response` | Standard API response wrapper (`Response<T>`) and status codes |
| `com.peach.common.exception` | Common business, parameter, and validation exceptions |
| `com.peach.common.audit` | Audit context contracts, snapshots, and `PeachDO` entity audit field filler |
| `com.peach.common.loader` | Custom SPI service loader (supports standard & custom paths, deduplication, thread-safe cache, and fault tolerance) |
| `com.peach.common.unique` | SPI-based ID generation framework (UUID without hyphen, NanoID, Snowflake) |
| `com.peach.common.key` | Public key definition contracts, metadata maintenance, and business-neutral key builder |
| `com.peach.common.util.desensitize` | Masking utilities for mobile, email, ID cards, IP, endpoints, and error messages |
| `com.peach.common.util.encrypt` | Symmetric encryption SPI (AES/DES/SM4), key resolvers, and hex codecs |
| `com.peach.common.util` | Base utilities for string, date/time, collections, and secure randomness |
| `com.peach.common.constant` | Cross-module shared constants |


## Public Key Maintenance

`peach-common` owns cross-module key definition contracts and key construction utilities. It does not store concrete business keys for captcha, login, order, cache scenarios, or other domain workflows.

- `KeyTemplate`: minimal key template contract exposing `pattern()`.
- `KeyDefinition`: key definition contract with metadata: `moduleCode()`, `keyIntroduce()`, `valueIntroduce()`, and `author()`.
- `KeyBuilder`: builds final keys from raw keys, string patterns, or `KeyTemplate` implementations.

Concrete business or component keys must live in the module that owns them. For example, CAPTCHA Redis keys are maintained by `com.peach.captcha.key.CaptchaRedisKey` in `peach-captcha-autoconfigure`, while reusing only `KeyDefinition` and `KeyBuilder` from `peach-common`.

## Audit Context

`PeachDO.fillCreateTime()` and `PeachDO.fillModifyTime()` read current user, tenant, and organization information via `AuditContext`. `peach-common` only defines the `AuditContextProvider` interface and does not directly depend on Sa-Token or any specific security framework.

The default Sa-Token bridge is provided by `peach-satoken-autoconfigure`. Applications can also declare a custom `AuditContextProvider` Spring Bean:

```java
@Bean
AuditContextProvider auditContextProvider() {
    return new AuditContextProvider() {
        @Override
        public String currentUserId() {
            return "system";
        }

        @Override
        public String currentTenantId() {
            return "default-tenant";
        }

        @Override
        public String currentOrgId() {
            return "default-org";
        }
    };
}
```

Thread, request, and login states are managed by the module providing the provider. `AuditContext` does not store request or user session objects.

## Symmetric Encryption

The encryption entry point is `EncryptFactory.getEncrypt(type)`. The current SPI provides:

- `EncryptConst.AES`: AES/GCM/NoPadding (ciphertext format `v1:AES-GCM:<hex>`).
- `EncryptConst.SM4`: SM4/GCM/NoPadding (ciphertext format `v1:SM4-GCM:<hex>`).
- `EncryptConst.DES`: Legacy DES/CBC for compatibility and migration.

Key resolution order: JVM system property &rarr; OS environment variable &rarr; classpath `default/*.key` file. Keys in `default/` serve only as fallback defaults to prevent startup crashes when unconfigured; production environments must explicitly supply keys.

| Algorithm | JVM System Property | Environment Variable | Key Length |
| --- | --- | --- | --- |
| AES | `peach.common.encrypt.aes.key` | `PEACH_COMMON_ENCRYPT_AES_KEY` | 16, 24, or 32 bytes |
| DES | `peach.common.encrypt.des.key` | `PEACH_COMMON_ENCRYPT_DES_KEY` | At least 8 bytes |
| SM4 | `peach.common.encrypt.sm4.key` | `PEACH_COMMON_ENCRYPT_SM4_KEY` | 16 bytes |

Keys can be provided as plaintext UTF-8 strings or prefixed with `base64:` for Base64-encoded binary key material.

## Desensitization & Error Sanitization

`DesensitizeUtil` provides masking operations for logs, error summaries, and unauthorized views. For exception messages, sanitize before logging or storing:

```java
String safeMessage = DesensitizeUtil.sanitizeErrorMessage(exception.getMessage(), 1000);
```

This cleans up newlines, masks common credential patterns (`password`, `token`, `secret`, `accessKey`, `Bearer`), and truncates to the specified maximum length.

## ID Generation

The unified entry point for ID generation is `UniqueIdFacade`. It leverages Java SPI (`ServiceLoader`) to discover `UniqueGeneratorProvider` implementations, providing out-of-the-box built-in algorithms and non-intrusive extension capabilities.

### 1. Built-in Algorithm Comparison

| Algorithm Type | Format & Characteristics | Typical Length | Ordering | Recommended Use Case |
| --- | --- | --- | --- | --- |
| `IdGeneratorConst.UUID` (`uuid`) | 32-character lowercase hex string without hyphens (`-`) | 32 chars | Non-ordered | Default algorithm; 100% backward compatible with legacy code |
| `IdGeneratorConst.NANOID` (`nanoid`) | 64 URL-safe characters (`0-9a-zA-Z_-`) | 21 chars | Non-ordered | Compact business identifiers, short URLs, multipart session keys |
| `IdGeneratorConst.SNOWFLAKE` (`snowflake`) | 64-bit long integer or numeric string | 19 digits | Trend increasing | High-throughput distributed primary keys, trace IDs, B+Tree friendly |

### 2. Configuration Parameters & Multi-Level Fallback Strategy

Configure global algorithms and Snowflake node IDs without modifying code via JVM system properties or OS environment variables.

| Configuration Item | JVM System Property | OS Environment Variable | Fallback Priority & Rules | Default Value |
| --- | --- | --- | --- | --- |
| **Global Default Algorithm** | `peach.common.id.type` | `PEACH_COMMON_ID_TYPE` | 1. System property<br>2. Environment variable<br>3. Warn log and fallback to default if unregistered<br>4. Fallback to first available provider | `uuid` |
| **Snowflake Worker ID** (worker-id) | `peach.common.id.snowflake.worker-id` | `PEACH_COMMON_ID_SNOWFLAKE_WORKER_ID`<br>(or short alias `PEACH_ID_WORKER_ID`) | 1. System property (0~31, invalid values are rejected)<br>2. Environment variable (0~31, invalid values are rejected)<br>3. Adaptive hash from local MAC / IP address<br>4. Ultimate fallback value | `1` |
| **Snowflake Datacenter ID** (datacenter-id) | `peach.common.id.snowflake.datacenter-id` | `PEACH_COMMON_ID_SNOWFLAKE_DATACENTER_ID`<br>(or short alias `PEACH_ID_DATACENTER_ID`) | 1. System property (0~31, invalid values are rejected)<br>2. Environment variable (0~31, invalid values are rejected)<br>3. Adaptive hash from local IPv4 address segment<br>4. Ultimate fallback value | `1` |

### 3. Code Usage Examples

```java
// 1. Generate ID using the global default algorithm (outputs 32-char UUID without hyphens by default)
String id = UniqueIdFacade.nextId();

// 2. Generate ID with a specific algorithm
String uuid = UniqueIdFacade.nextId(IdGeneratorConst.UUID);
String nanoId = UniqueIdFacade.nextId(IdGeneratorConst.NANOID);
String snowflakeStr = UniqueIdFacade.nextId(IdGeneratorConst.SNOWFLAKE);

// 3. Convenience helper methods
String quickNano = UniqueIdFacade.generateNanoId();
String quickSnow = UniqueIdFacade.generateSnowflakeId();
long numericSnowflakeId = UniqueIdFacade.nextLongId(); // 64-bit long Snowflake ID

// 4. Legacy compatibility method (retained for backward compatibility)
String legacyUuid = IDGeneratorUtil.generateUuid();
```

`IDGeneratorUtil` is retained as a legacy compatibility entry point. New code should prefer `UniqueIdFacade`.

### 4. Custom Algorithm SPI Extension

To integrate a custom generator (e.g. Meituan Leaf, Baidu UidGenerator, or domain-prefixed encoders) without modifying `peach-common`:

1. Implement the `UniqueGenerator` interface and define its algorithm identifier (e.g. `type() { return "leaf"; }`).
2. Implement the `UniqueGeneratorProvider` interface returning the generator instance.
3. Declare the fully-qualified class name in `src/main/resources/META-INF/services/com.peach.common.unique.IdGeneratorProvider`.
4. Call `UniqueIdFacade.nextId("leaf")` directly, or configure `-Dpeach.common.id.type=leaf` as the global default.

## Custom Service Loader (CustomServiceLoader)

`CustomServiceLoader` is an enhanced SPI service loader based on JDK `ServiceLoader`, providing unified SPI service discovery, caching, and extension capabilities.

### Key Features
- **Standard Compatibility**: Seamlessly loads standard SPI files from `META-INF/services/`.
- **Custom Path Support**: Supports custom classpath locations (directory prefix like `"META-INF/peach-services/"` or specific file path), matching and reading configurations safely.
- **Deduplication**: Retains first-discovered providers when duplicates exist across standard and custom paths, ensuring each implementation class is instantiated only once while preserving discovery order.
- **Thread-Safe Caching**: Caches loaded provider lists by `(serviceClass, classLoader, customPaths)` to avoid repeated ClassLoader resource scans and reflection.
- **Fault Tolerance**: Configuration line syntax errors, non-existent classes, or instantiation exceptions are safely isolated with warning logs; supports inline `#` comments.
- **Convenient APIs**: Supports `load(Class<T>)`, `findFirst(Class<T>)`, `loadFirst(Class<T>, T default)`, `reload(Class<T>)`, and `loadFromFile(Class<T>, File)`.

### Usage Examples

```java
// 1. Load all available providers with automatic caching
List<IdGeneratorProvider> providers = CustomServiceLoader.load(IdGeneratorProvider.class);

// 2. Load the first available provider (or fallback if none)
EncryptProvider provider = CustomServiceLoader.loadFirst(EncryptProvider.class, defaultProvider);

// 3. Load from custom resource directory
List<StorageProviderFactory> factories = CustomServiceLoader.load(
        StorageProviderFactory.class, "META-INF/peach-storage/");

// 4. Invalidate cache and reload
List<IdGeneratorProvider> reloaded = CustomServiceLoader.reload(IdGeneratorProvider.class);
```

## Module Boundaries

- Do NOT place business cache keys, MQ topics, database table column names, domain status codes, or business workflows here; common only owns business-neutral key contracts such as `KeyDefinition`, `KeyTemplate`, and `KeyBuilder`.
- Do NOT place Web global exception handlers, servlet filters, interceptors, or application startup classes here.
- Do NOT bypass module dependency boundaries via reflection or static application context lookups.
- Do NOT commit real production passwords, tokens, private keys, signed URLs, or internal IP endpoints to source files or documentation.
- Verify real cross-module adoption before introducing new public APIs.

## Verification

Recommended verification commands:

```bash
node scripts/check-utf8.mjs
mvn -pl peach-common -am test -Pdevelopment
git diff --check
```

When changing audit contracts or Sa-Token bridge auto-configuration:

```bash
mvn -pl peach-middleware/peach-satoken/peach-satoken-autoconfigure -am test -Pdevelopment
```

Ensure `JAVA_HOME` points to JDK 21 before running Maven commands.

## Troubleshooting

| Symptom | Checkpoint | Resolution |
| --- | --- | --- |
| `encryption key is not configured` during encryption | Whether system properties, env vars, or classpath default keys are empty | Configure explicit keys; default keys are only for local development |
| Decryption failure for legacy data | Whether ciphertext is legacy CBC format and key matches historical data | Maintain old key accessibility, then migrate to `v1:` ciphertext format |
| Audit fields (`creatorId`, etc.) are empty | Whether `AuditContextProvider` bean exists and is registered by auto-configuration | Verify `peach-satoken-starter` or custom provider configuration |
| Missing tenant or organization context | Whether entity has writable `tenantId`/`orgId` fields and provider returns non-empty values | Ensure tenant and org context validation occurred in service layer |
| `No ID generator provider found for type: ...` | Whether algorithm name is spelled correctly and declared in SPI file | Verify algorithm name or manually register via `IdGeneratorRegistry.register()` |
| `Clock moved backwards` in Snowflake | System clock experienced backward drift (>5ms) or sudden timezone shift | Check server NTP sync status; avoid abrupt backward clock adjustments |
| Downstream module compilation failure | Public method signatures, constants, or exception constructors modified | Use `rg` and CodeGraph to verify downstream callers and maintain compatibility |
