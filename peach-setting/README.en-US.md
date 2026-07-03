# peach-setting

English | [中文](README.md)

## Purpose

`peach-setting` is the system-setting domain. It manages dictionaries, value sets, notices, localized messages, and exposes OpenFeign APIs for other services.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-setting-service` | Dictionary, value-set, notice, and localized-message services |
| `peach-setting-rest` | Internal REST management APIs |
| `peach-setting-launch` | Runtime application module |
| `peach-setting-entity` | DO/DTO/QO/VO models |
| `peach-setting-common` | Shared setting-domain objects |
| `peach-setting-openfeign-external` | External OpenFeign client |

## Key Entrypoints

- Application: `peach-setting-launch/src/main/java/com/peach/setting/launch/PeachSettingApplication.java`
- REST controllers: `peach-setting-rest/src/main/java/com/peach/setting/rest/internal`
- Service interfaces: `peach-setting-service/src/main/java/com/peach/setting/service`
- Feign client: `peach-setting-openfeign-external/src/main/java/com/peach/setting/openfeign`

## Boundaries

- This module manages configuration data; it does not define all real-time push protocols for business configuration.
- Cache refresh strategies for dictionaries and value sets must be designed by each consumer scenario.

## Verification

```bash
mvn -f "peach-setting/pom.xml" -DskipTests package
```
