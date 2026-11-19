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

### #231 Historical file-object and upload-session scope guard (superseded by #232)

File pagination already populates a business tenant/organization scope through `AuditContext.requireTenantId/requireOrgId`, but direct `selectByFileId`, `requireRecord` and `requireSession` calls previously did not check row ownership. A guessed file or upload-session ID could therefore reach a different tenant's record. `FileRecordScopeGuard` now requires an exact match between the server's existing trusted business tenant/organization and the persisted `TENANT_ID/ORG_ID` **before returning details, generating presigned download URLs, deleting/restoring records, or using upload sessions**. The file detail SQL includes ownership fields; missing or mismatched scopes fail closed. Background cleanup remains a separate trusted system activity, never a fabricated user session. Focused tests cover cross-tenant, cross-organization and missing scopes.

**Scope limitation:** This is a stopgap on existing business IDs, not completed SaaS physical `tenantRefId/memberRefId/appRefId` isolation, full field/row authorization, or replacement of the old Redis user profile. Full database migration and end-to-end object storage checks are tracked in #232 and #211; passing this code's CI is not production authorization acceptance.

The `FileRecordDao.logicalDelete` and `restoreByFileId` MyBatis SQL `WHERE` clauses additionally bind the verified row's `TENANT_ID` and `ORG_ID` and require exactly one affected row. Concurrent scope or soft-delete status changes therefore fail rather than falling back to an update by `FILE_ID` alone.

### #232 Physical SaaS file isolation (development in progress)

**Foundation committed on the feature branch:** file records, objects and upload sessions gain UUID physical `ID` keys while retaining unique permanent `FILE_ID`, `OBJECT_ID` and `SESSION_ID` business identifiers. Trusted `TENANT_REF_ID`, `APP_REF_ID`, `OWNER_USER_REF_ID`, optional `ORG_REF_ID` and upload `MEMBER_REF_ID` are carried by persistence models. `SaasIamFeignClient` checks the actual end-user Token and API action against Auth; Auth validates the current member/grant/password versions. Failed Same-Token, Feign or Redis checks **deny** instead of falling back to caller-supplied business tenant IDs or the legacy USER_PROFILE cache.

- Instant deduplication SQL is restricted by physical tenant, application and owner; a shared SHA-256 cannot authorize cross-tenant reuse. File list and COUNT queries enforce the same physical range before pagination. Detail and signed-download URL issuance use scoped SQL. Delete/restore UPDATE statements require the same physical tuple; upload-session continuation requires the original physical member.
- File lists now apply Auth's bounded same-binding SELF/ORG/ORG_TREE/TENANT_ALL row intersections and unions before COUNT and pagination; cross-owner access requires an explicitly valid grant.
- `sql/PEACH_FILE_SAAS_PHYSICAL_MIGRATION.sql` is an **existing-database one-time migration** requiring DBA backup, primary-key cutover and evidence-based historical tenant/app/owner/member reconciliation. Unmapped rows remain NULL and are intentionally invisible to new scoped SQL; never guess mappings or interpret legacy TENANT_ID as a physical ID. New databases use the updated `sql/PEACH_FILE_*.sql` or `sql/ALL_TABLE_CREATE.sql` and must not replay the ALTER migration.
- `sql/PEACH_FILE_SAAS_API_PERMISSIONS.sql` creates SYSTEM API definitions but grants no authority. Old wildcard paths such as `GET:/api/file/internal/*` are not accepted by the current exact/parameterized matcher; replace them with protected `{fileId}` patterns and least-privilege grants. External proxy calls currently reuse internal domain operations and consequently need their internal action permissions too.
- Background cleanup is separate from user requests and only processes expired records with validated physical mapping; it does not fabricate user tokens or reclaim unmapped historical data.
- Live MySQL, Gateway, Auth Feign Same-Token, Redis/Nacos, full row/field enforcement, provider URL behavior and historical migration still require #211 acceptance. **This section records implementation progress, not #232 completion or production readiness.**


#### Same-role-binding file row policies

The internal `/auth/external/iam/file-object/check` IAM endpoint requires a separate Same-Token service identity, the genuine end-user token and the file service's database-backed physical object, tenant, app, owner and organization IDs. Auth revalidates current membership, grant and password versions before intersecting a specific `DATA` action (`FILE.LIST`, `FILE.READ`, `FILE.DOWNLOAD`, `FILE.UPLOAD`, `FILE.DELETE` or `FILE.RESTORE`) with its `PEACH_ROLE_DATA_POLICY` in the **same role binding**. API permission alone cannot grant row access, and missing data policy denies by default. Physical tenant/app SQL conditions remain unconditional; cross-owner access requires a valid same-binding organization or tenant-wide row policy.

An authorized administrator must separately grant each DATA permission and configure its row policy (for example SELF) via existing protected role/policy flows. Importing the directory alone grants nothing. Organization-tree/field read-write masking and complete multi-binding SQL list unions are still outstanding in #232.

#### File detail and list field permissions (integrated)

`SaasIamFeignClient.projectFileFields` uses trusted Same-Token to evaluate `PEACH_ROLE_FIELD_POLICY` for the `FILE_RECORD` resource, intersecting the *same* authorized role binding and `FILE.READ` / `FILE.LIST` row scope. Only explicitly `ALLOW` or `MASK` fields are copied into the public `FileRecordVO`. `DENY` or unknown fields are omitted. The `fileId` field itself must have explicit read permission; otherwise detail/list access fails closed. Internal storage provider, bucket/objectKey and physical owner/tenant/app identifiers are never included.

Pages are capped at 50. Physical MySQL tenant/app/owner filters and COUNT run before pagination. Each row is independently evaluated against its valid role binding, while a **single batched Feign request** shares the current member's DATA grants and field policies. Any unauthorized row denies the entire page rather than producing a misleading partially filtered page with the original COUNT. Live MySQL/Feign performance and authorization-version consistency still require acceptance testing. Scoped organization-tree SQL unions and controlled metadata PATCH write checks are integrated; live MySQL, storage-provider, gateway and migration acceptance remain open in #211.

#### #232 Current organization row scopes and metadata writes

Auth intersects the row scope and DATA policy independently inside each effective member-role binding, then unions valid `SELF`, `ORG`, `ORG_TREE`, and `TENANT_ALL` scopes. Fileservice only accepts a scope matching the currently verified physical tenant, app and user; the fixed parameterized MyBatis SQL filters records before COUNT and pagination. Organization expansion is capped at 512 IDs, failures deny by default, and pages are capped at 50 rows before batch field projection. Deleted-file listing is currently denied.

The new `PATCH /file/internal/{fileId}/metadata` endpoint accepts:

```json
{"changes":{"displayName":"New name","bizTag":"tag","remark":"notes"}}
```

Only these three fields are writable; values must be non-null and no longer than 255, 64 and 500 characters respectively. The PATCH action permission, the effective same-binding `FILE.EDIT` DATA row policy and explicit `WRITE_EFFECT=ALLOW` for every requested field are all required. The fixed UPDATE rechecks persisted tenant/app, owner/org, active state and soft-delete status. Permission definitions do not automatically grant any role or field policy.

The permanent business tenant number is now supplied from Auth's own tenant database through `SaasIamContextVO.tenantBusinessId`. Fileservice no longer directly queries the Auth tenant table. Historical unmapped file ownership remains quarantined pending #211 validation.

#### #232 Bounded atomic batch delete/restore

`POST /file/internal/batch/delete` and `POST /file/internal/batch/restore` accept 1–50 distinct permanent file IDs:

```json
{"fileIds":["FILE00001","FILE00002"]}
```

Each call requires its separate protected SYSTEM batch API action. **Every file** additionally requires the corresponding single-file DELETE/RESTORE API and same-binding `FILE.DELETE` / `FILE.RESTORE` DATA row authorization. Fixed MyBatis WHERE predicates recheck tenant/app and stored owner/org. Any authorization or concurrent update conflict rolls back the entire local database transaction. Physical object-store cleanup is a separate background workflow and is not automatically rolled back with MySQL.

#### Protected CSV metadata export

`POST /file/internal/export` accepts the same business filters as the file list and returns an UTF-8 `text/csv` attachment. It first requires its own SYSTEM API action `POST:/api/file/internal/export`, then revalidates the list API action, the same-binding `FILE.LIST` row scope applied in MyBatis before COUNT and pagination, and the `FILE_RECORD` field `ALLOW / MASK / DENY` projection for every row. Browser-supplied tenant or organization scopes are never trusted.

Exports are capped at **500 rows**, fetched in pages of 50. Oversized exports, inconsistent page totals, incomplete reads, or failed field authorization deny the entire export. The output contains only permitted fixed metadata fields, never bucket, objectKey, provider or physical ownership identifiers; formula-like spreadsheet values are escaped to mitigate CSV injection. The export API catalog entry grants no roles automatically.
