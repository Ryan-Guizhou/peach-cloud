# peach-sample

English | [中文](README.md)

## Purpose

`peach-sample` is a sample application for validating and demonstrating components and middleware such as distributed locks, delay queues, captcha, email, Bloom filters, and multi-level cache.

## Current Samples

| Package/Controller | Capability |
| --- | --- |
| `DistributedLockController` | Redisson distributed lock |
| `AdobeController` | Delay queue sample |
| `CaptchaController` | Captcha generation and verification |
| `EmailSendController` | Email sending |
| `BloomController` | Bloom filter |
| `MulticacheController` | Multi-level cache |

## Entrypoint

- Application: `peach-sample/src/main/java/com/peach/sample/SampleApplication.java`

## Usage Notes

- This module is for development and verification, not direct production deployment.
- Redis, Redisson, email, captcha, and other sample dependencies must be configured for the local environment.
- New starters should add a minimal runnable sample here.

## Verification

```bash
mvn -f "peach-sample/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
