# peach-captcha

English | [中文](README.md)

## Purpose

`peach-captcha` is a captcha starter providing text captcha, click-word captcha, slider/puzzle captcha, rotate puzzle captcha, and knowledge-question captcha. It supports in-memory or Redis-backed cache.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-captcha-autoconfigure` | Captcha services, cache, providers, and auto-configuration |
| `peach-captcha-starter` | Starter exposed to business modules |

## Core Objects

- `CaptchaProperties`: captcha configuration binding.
- `CaptchaService`: generation and verification entrypoint.
- `CaptchaServiceFactory`: routes by captcha type.
- `CaptchaCacheService`: captcha cache abstraction.
- `MemoryCaptchaCacheService` / `RedisCaptchaCacheService`: cache implementations.
- `CaptchaServiceProvider`: provider extension point.

## Supported Types

- Text captcha: `TextCaptchaServiceImpl`
- Click-word captcha: `ClickWordCaptchServiceImpl`
- Block puzzle captcha: `BlockPuzzleCaptchaServiceImpl`
- Rotate puzzle captcha: `RotatePuzzleCaptchaServiceImpl`
- Knowledge captcha: `KnowledgeCaptchaServiceImpl`

## Boundaries

- In-memory cache is suitable only for single-instance development.
- Cluster deployment should use Redis cache.
- Anti-bruteforce, replay protection, and frequency control must be combined with `FrequencyLimitHandler` and gateway rate limits.
- Images, fonts, and noise parameters should be tuned for product experience.

## Verification

```bash
mvn -f "peach-component/peach-captcha/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
