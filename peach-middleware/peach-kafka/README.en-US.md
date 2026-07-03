# peach-kafka

English | [中文](README.md)

## Purpose

`peach-kafka` is the Kafka middleware module. The current directory contains an aggregator `pom.xml` and README only; no autoconfigure, starter, or source implementation was found.

## Current State

- This module is currently a placeholder/planned module.
- Business modules must not assume a Kafka starter, auto-configuration class, or unified producer/consumer API exists.
- Future implementation should add starter/autoconfigure modules, configuration properties, producer/consumer APIs, examples, and tests.

## Suggested Boundaries

- Decide whether it is based on Spring Kafka.
- Provide unified producer, consumer registration, and error handling.
- Define topic, consumer group, serialization, retry, dead-letter, and idempotency boundaries.

## Verification

```bash
mvn -f "peach-middleware/peach-kafka/pom.xml" -DskipTests package
```
