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
