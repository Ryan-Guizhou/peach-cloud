# peach-generator

English | [中文](README.md)

## Purpose

`peach-generator` is the code-generation domain. It manages datasources, database metadata, generator configuration, templates, preview, and generation execution. The module also contains `DESIGN.md` and is split into common, entity, service, rest, and launch submodules.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-generator-common` | Shared constants, utilities, and generator context objects |
| `peach-generator-entity` | Datasource, table metadata, column metadata, template, and config models |
| `peach-generator-service` | Datasource, metadata, config, template, preview, and generation services |
| `peach-generator-rest` | Code-generation REST APIs |
| `peach-generator-launch` | Runtime application module |

## Main Capabilities

- Multiple datasource configuration and connection testing.
- Database table, column, and primary-key metadata loading.
- Generation configuration management.
- Template management and default templates.
- Code preview and generation execution.
- Extensible generated output for files or target-module integration.

## Key Entrypoints

- Design document: `peach-generator/DESIGN.md`
- Application: `peach-generator-launch/src/main/java/com/peach/generator/launch/PeachGeneratorApplication.java`
- REST controllers: `peach-generator-rest/src/main/java/com/peach/generator/rest/internal`
- Service interfaces: `peach-generator-service/src/main/java/com/peach/generator/service`
- Default templates: `peach-generator-service/src/main/java/com/peach/generator/service/engine/GenDefaultTemplates.java`

## Boundaries

- The generator cannot guarantee that generated code satisfies every business convention; templates and configuration must be maintained with project rules.
- External datasource credentials must be environment-specific and must not be committed.
- Generated code still needs compilation, formatting, and review in the target module.

## Verification

```bash
mvn -f "peach-generator/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
