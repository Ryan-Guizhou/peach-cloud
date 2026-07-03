# peach-redission

English | [中文](README.md)

## Purpose

`peach-redission` wraps Redisson-based distributed locks, repeat-submit protection, delay queues, and Bloom filters. The module name follows the current repository spelling `redission`; the underlying dependency is Redisson.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-redission-common` | Common lock-info handling and Redisson shared auto-configuration |
| `peach-redission-distributedlock-*` | Distributed-lock annotation, aspect, and lock handlers |
| `peach-redission-repeat-*` | Repeat-submit protection |
| `peach-redission-delayqueue-*` | Delay queue publishing, consuming, and configuration |
| `peach-redission-bloomfilter-*` | Segmented Bloom filter and SPI extensions |

## Core Capabilities

- `DistributedLocker`: distributed lock execution entrypoint.
- `LockInfoHandle`: lock information building and extension.
- `DelayQueueProperties` / `ConsumerTask`: delay queue configuration and consumption.
- `BloomFilterService`: Bloom filter add/check and segmented scaling.
- `KeyNamingStrategy`, `CodecProvider`, `BloomScalePolicy`: Bloom filter SPI.

## Boundaries

- This module depends on an external `RedissonClient`, usually provided by `peach-redis`.
- Distributed locks protect only Redis-reachable critical sections and do not replace database constraints or business idempotency.
- Bloom filters may return false positives and must not be used as exact existence checks.
- Delay queue retry, dead-letter, and compensation policies must be implemented by business code.

## Verification

```bash
mvn -f "peach-middleware/peach-redission/pom.xml" -DskipTests package
```
