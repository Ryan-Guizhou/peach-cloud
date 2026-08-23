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


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
