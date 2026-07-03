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
