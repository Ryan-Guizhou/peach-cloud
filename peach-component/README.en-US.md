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
