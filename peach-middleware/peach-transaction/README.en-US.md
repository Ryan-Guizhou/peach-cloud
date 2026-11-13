# peach-transaction

English | [中文](README.md)

An extensible distributed transaction integration starter for Peach Cloud. Currently it integrates **Seata 2.5.0 AT only**. TCC, XA and Saga are future options, **not implemented**. Existing RocketMQ transactional messages and Outbox are reused rather than reimplemented.

## Modules and capability matrix

| Module | Responsibility |
| --- | --- |
| `peach-transaction-autoconfigure` | Public SPI, transaction routing, properties, startup checks and metadata |
| `peach-transaction-seata` | Seata AT SPI provider backed by the official SCA starter |
| `peach-transaction-starter` | Application dependency entry point |
| `peach-transaction-quickstart` | Infrastructure-free wiring smoke test; not a two-DB rollback test |

| Capability | Status |
| --- | --- |
| Official `@GlobalTransactional` + AT | Integration included; real TC/MySQL validation pending |
| `GlobalTransactionExecutor` / `GlobalTransactionOperations` SPI | Implemented |
| User Bean override for SPI provider | Implemented |
| Feign XID propagation / DataSource auto-proxy | Delegated to SCA/Seata; cross-service validation pending |
| TCC / Saga / XA | Not implemented |
| Outbox and consumer idempotency | Reuse peach-rocket |
| AT + Outbox startup safety check | Fails closed by default; does not implement global commit fencing |

## Installation

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-transaction-starter</artifactId>
</dependency>
```

Deploy a separate **Seata TC server**, make Nacos/network connectivity available and install the official `undo_log` schema in every participating AT database. Seata must proxy the business DataSource. The project uses Java 21, Boot 3.5.4, and SCA 2025.0.0.0 (Seata 2.5.0 per the BOM).

```yaml
peach:
  transaction:
    enabled: true
    provider: seata
    mode: AT
    timeout-millis: 60000
seata:
  enabled: true
  tx-service-group: peach-cloud-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER_ADDR}
      application: seata-server
      group: SEATA_GROUP
  service:
    vgroup-mapping:
      peach-cloud-tx-group: default
```

Check server deployment, group mapping and configuration-center settings for your environment. To turn the integration off, set **both** `peach.transaction.enabled=false` and `seata.enabled=false`: the latter prevents the third-party Seata starter from initializing even when the Peach facade is disabled.

For low-intrusion business code use the official `@GlobalTransactional(rollbackFor = Exception.class)` on a Spring-proxied entry point. Self invocation is not intercepted. The optional programming SPI is:

```java
TransactionOptions options = new TransactionOptions("update-account", TransactionMode.AT, 60000);
operations.execute(options, () -> {
    accountMapper.update(...);
    profileClient.update(...);
    return null;
});
```

The example methods are illustrative. Ordinary `@Transactional` alone cannot cover remote services. Downstream participants must receive XID and preserve rollback-signalling exceptions.

## Safety boundaries

- AT locally commits branch data before global completion. The RocketMQ JDBC Outbox Dispatcher may publish an event before global rollback. Enabling `peach.rocket.outbox.enabled` alongside AT therefore fails startup by default. Only set `peach.transaction.allow-at-outbox-coexistence=true` after independently designing and verifying a durable global-commit publish fence; **the flag does not implement a fence**.
- AT cannot undo MinIO/OSS, MongoDB, Redis or MQ side effects. Use reliable events or separately implemented persistent Saga compensation.
- Disable blind retries for non-idempotent Feign writes. Do not use Sentinel fallback to hide failures. Strip untrusted Seata XID headers at the edge and propagate them only among authorized internal services. This starter does not modify existing gateway/Feign interceptors; verify before production.
- SPI execution uses thread-bound transaction context. Async/virtual threads do not automatically inherit XID. Existing parent transactions are not committed by nested operations.
- Exactly one named provider is selected. Unsupported modes and unknown providers fail immediately; do not silently fall back to local transactions.

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `peach.transaction.enabled` | `false` | Enable Peach facade |
| `peach.transaction.provider` | `seata` | SPI provider |
| `peach.transaction.mode` | `AT` | Currently implemented mode |
| `peach.transaction.timeout-millis` | `60000` | Default timeout in milliseconds |
| `peach.transaction.allow-at-outbox-coexistence` | `false` | Block unsafe AT/Outbox coexistence by default |

Peach properties are described in `additional-spring-configuration-metadata.json`; Seata properties are provided by the vendor starter.

## Verification

```bash
mvn -pl peach-middleware/peach-transaction -am test -Pdevelopment
mvn -pl peach-middleware/peach-transaction/peach-transaction-quickstart -am spring-boot:run -Pdevelopment
```

The quickstart disables Seata by default and only checks dependency wiring/application startup. **It does not validate actual AT branches, Feign XID propagation or rollback across two MySQL databases.** Full integration acceptance requires Seata TC/Nacos and two actual services with failure injection for successful commits, branch errors, TC outage, retries and rollback failure. Official Seata deployment/undo_log guide: https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-guide-beginner/
