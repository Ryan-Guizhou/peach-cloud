# peach-common

English | [中文](README.md)

## Purpose

`peach-common` is the shared backend foundation module. It contains cross-domain constants, response models, exception handling, pagination models, utilities, and base annotations. Business modules may depend on it, but it must not depend on concrete business domains.

## Contents

- `com.peach.common.response`: unified response model.
- `com.peach.common.exception`: global exception handling and exception types.
- `com.peach.common.constant`: service names, service paths, and shared constants.
- `com.peach.common.util`: encryption, date, ID, and other utilities.
- Shared pagination and base DTO/VO support objects.

## Usage Rules

- Keep only stable cross-module foundations here.
- Do not place auth, file, message, or other domain-specific logic here.
- Changes to common exceptions or response models must be checked against all services.

## Verification

```bash
mvn -f "peach-common/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
