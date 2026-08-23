# peach-component

English | [中文](README.md)

## Purpose

`peach-component` aggregates reusable components that are not tied to a specific business domain.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-captcha` | Image captcha, click-word captcha, slider/puzzle captcha capabilities |
| `peach-email` | Email sending, templates, retry, idempotency, and provider routing |
| `peach-storage` | Unified storage abstraction for local, SFTP/NAS, OSS/S3/MinIO, and other providers |
| `peach-initialize` | Application startup initialization task orchestration |
| `peach-threadpool` | Configurable thread pools and `@AsyncExecuted` annotation |

## Usage Rules

- Business services should import concrete starters only.
- Starters expose public entrypoints; autoconfigure modules provide auto-configuration and defaults.
- New components must include README, configuration, boundaries, and build verification commands.

## Verification

```bash
mvn -f "peach-component/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
