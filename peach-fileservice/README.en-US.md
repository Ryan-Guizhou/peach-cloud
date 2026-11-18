# peach-fileservice

English | [中文](README.md)

Last updated: 2026-08-12
Runtime baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0

## Purpose

`peach-fileservice` is the file-domain service. It coordinates file records, object references, upload sessions, and object-storage calls through `MultiZoneStorage` from `peach-store-starter`.

It provides internal file management APIs, internal storage administration APIs, external service-to-service file APIs, `FileFeignClient`, and cleanup jobs for expired deleted files and upload sessions.

It does not provide vendor SDK abstraction, business authorization, tenant isolation, download auditing, CDN invalidation, content review, virus-scanning integration, or distributed transactions between the database and object storage.

## API boundaries

Internal APIs are management or local service APIs:

| Controller | Prefix | Responsibility |
| --- | --- | --- |
| `FileController` | `/file/internal` | Upload, pre-check, detail, download URL, page query, logical delete, restore |
| `FileMultipartController` | `/file/internal/multipart` | Multipart init, part URL, complete, abort |
| `FileToolController` | `/file/internal/tools` | Internal file utilities |
| `CloudStorageInstanceController` | `/file/internal/storage/instance` | Storage instance configuration |
| `CloudStorageBrowserController` | `/file/internal/storage/browser` | Object browsing and object operations |
| `HealthController` | `/file/health` | Health check |

External APIs are stable service-to-service contracts under `/file/external` and are mirrored by `FileFeignClient`:

| Method | Path | Feign method | Description |
| --- | --- | --- | --- |
| `POST` | `/file/external/upload` | `upload` | Upload a business file and calculate SHA-256 on the server |
| `POST` | `/file/external/tools/sha256` | `sha256` | Calculate a digest without storing the object |
| `GET` | `/file/external/{fileId}` | `detail` | Return storage-redacted file details |
| `GET` | `/file/external/{fileId}/url` | `getUrl` | Return a temporary download URL |
| `DELETE` | `/file/external/{fileId}` | `delete` | Logically delete by business file ID |

External APIs never accept bucket names, object keys, local paths, target paths, source directories, or caller-machine paths.

## Module structure

```text
peach-fileservice/
├── peach-fileservice-common/
├── peach-fileservice-entity/
├── peach-fileservice-service/
├── peach-fileservice-rest/
├── peach-fileservice-openfeign-external/
├── peach-fileservice-launch/
├── pom.xml
├── README.md
└── README.en-US.md
```

## Configuration

`FileDomainProperties` binds `peach.file`:

| Property | Default | Description |
| --- | --- | --- |
| `peach.file.default-provider` | Not declared | Default storage provider |
| `peach.file.object-key-prefix` | `files` | Generated object-key prefix |
| `peach.file.retention-days` | `30` | Logical-delete retention days |
| `peach.file.download-url-expire-seconds` | `3600` | Download URL lifetime |
| `peach.file.part-url-expire-seconds` | `900` | Multipart part URL lifetime |
| `peach.file.upload-session-expire-minutes` | `120` | Upload-session timeout |
| `peach.file.cleanup-enabled` | `true` | Enable deleted-file cleanup |
| `peach.file.cleanup-cron` | `0 0 3 * * ?` | Deleted-file cleanup cron |
| `peach.file.upload-session-cleanup-enabled` | `true` | Enable expired-session cleanup |
| `peach.file.upload-session-cleanup-cron` | `0 0/30 * * * ?` | Session cleanup cron |

## Safety notes

- Protect both `/file/internal/**` and `/file/external/**` with authentication, authorization, and gateway routing.
- External APIs return redacted file data and do not expose bucket, objectKey, or provider internals.
- Presigned URLs are temporary credentials. Do not log, audit, persist, or cache them long-term.
- Storage instance configuration can contain sensitive endpoint or credential data; audit only non-sensitive identifiers.
- Ordinary upload reads full multipart bytes; use multipart direct upload for large files.
- ClamAV classes exist but are not wired into the upload path.

## Build and verification

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -pl peach-fileservice/peach-fileservice-launch -am -DskipTests package -Pdevelopment
```

The Maven command verifies compilation and packaging only. Real provider upload, multipart direct upload, authorization, and cleanup behavior require integration testing.


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.

### #231 Legacy file-object and upload-session scope guard

File pagination already populates a business tenant/organization scope through `AuditContext.requireTenantId/requireOrgId`, but direct `selectByFileId`, `requireRecord` and `requireSession` calls previously did not check row ownership. A guessed file or upload-session ID could therefore reach a different tenant's record. `FileRecordScopeGuard` now requires an exact match between the server's existing trusted business tenant/organization and the persisted `TENANT_ID/ORG_ID` **before returning details, generating presigned download URLs, deleting/restoring records, or using upload sessions**. The file detail SQL includes ownership fields; missing or mismatched scopes fail closed. Background cleanup remains a separate trusted system activity, never a fabricated user session. Focused tests cover cross-tenant, cross-organization and missing scopes.

**Scope limitation:** This is a stopgap on existing business IDs, not completed SaaS physical `tenantRefId/memberRefId/appRefId` isolation, full field/row authorization, or replacement of the old Redis user profile. Full database migration and end-to-end object storage checks are tracked in #232 and #211; passing this code's CI is not production authorization acceptance.

The `FileRecordDao.logicalDelete` and `restoreByFileId` MyBatis SQL `WHERE` clauses additionally bind the verified row's `TENANT_ID` and `ORG_ID` and require exactly one affected row. Concurrent scope or soft-delete status changes therefore fail rather than falling back to an update by `FILE_ID` alone.
