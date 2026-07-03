# peach-fileservice

English | [中文](README.md)

## Purpose

`peach-fileservice` is the file-service domain. It manages file metadata, upload/download entrypoints, storage-provider calls, and external OpenFeign APIs. Low-level storage capabilities are mainly provided by `peach-storage`.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-fileservice-service` | File domain services, storage manager, and business logic |
| `peach-fileservice-rest` | REST APIs for upload, download, and query operations |
| `peach-fileservice-launch` | Runtime application module |
| `peach-fileservice-entity` | File DO/DTO/QO/VO models |
| `peach-fileservice-common` | Shared file-domain objects |
| `peach-fileservice-openfeign-external` | OpenFeign client for file service |

## Key Entrypoints

- Application: `peach-fileservice-launch/src/main/java/com/peach/fileservice/launch/PeachFileserviceApplication.java`
- REST controller: `peach-fileservice-rest/src/main/java/com/peach/fileservice/rest/internal/FileController.java`
- Domain service: `peach-fileservice-service/src/main/java/com/peach/fileservice/service/IFileDomainService.java`
- Storage manager: `peach-fileservice-service/src/main/java/com/peach/fileservice/manager/CloudStorageManagerService.java`
- Configuration prefix: `peach.file.*`, bound by `FileDomainProperties`

## Boundaries

- This module should not implement every vendor SDK directly; provider differences belong in `peach-storage`.
- File permissions, temporary URLs, and download auditing must be designed per business scenario.
- Large-file upload, multipart upload, and frontend direct upload depend on the selected storage provider.

## Verification

```bash
mvn -f "peach-fileservice/pom.xml" -DskipTests package
```
