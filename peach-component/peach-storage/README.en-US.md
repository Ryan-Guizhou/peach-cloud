# peach-storage

English | [中文](README.md)

## Purpose

`peach-storage` is a unified storage starter. It exposes `StorageTemplate` to hide differences between local files, NAS/SFTP, OSS, OBS, S3, MinIO, COS, BOS, Ceph, and other providers. It supports upload, download, delete, list, copy, move, presigned URLs, frontend direct upload, and multipart upload.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-store-autoconfigure` | Core APIs, request/response models, provider SPI, auto-configuration, and defaults |
| `peach-store-starter` | Starter exposed to business modules |
| `peach-store-quickstart` | Minimal LOCAL-provider loop; production modules must not depend on it |

## Core Objects

- `StorageTemplate`: unified business entrypoint.
- `StorageProvider`: runtime storage SPI.
- `StorageProviderFactory`: startup provider creation and validation SPI.
- `StorageProviderRegistry`: provider registration, lookup, and shutdown.
- `StorageProperties`: binds `peach.storage.*`.
- Request objects such as `UploadObjectRequest`, `DownloadObjectRequest`, `PresignedUrlRequest`.
- Response objects such as `UploadResult`, `ObjectInfo`, `ListObjectsResult`.

## Configuration Example

```yaml
peach:
  storage:
    enabled: true
    primary: local
    providers:
      local:
        type: LOCAL
        bucket-name: app-local
        root-path: D:/data/peach-storage
        prefix: dev/app
        domain: http://localhost/files
```

## Usage Example

```java
@Resource
private StorageTemplate storageTemplate;

public UploadResult upload(UploadContent content) {
    return storageTemplate.upload(UploadObjectRequest.builder()
            .objectKey("docs/readme.txt")
            .content(content)
            .contentType(StorageContentType.TEXT_PLAIN_UTF8)
            .build());
}
```

Named provider:

```java
storageTemplate.upload("archive", request);
storageTemplate.download("archive", downloadRequest);
```

## Quick Start (runnable sample)

[`peach-store-quickstart`](./peach-store-quickstart/) is non-web. It depends on `peach-store-starter` only and uses the LOCAL provider plus a temp directory. No cloud credentials are required. Production modules must not depend on the quickstart.

The public entry is the auto-configured `StorageTemplate` (`PeachStorageAutoConfiguration`). The sample injects that bean and covers three LOCAL-supported cases:

| Item | Detail |
| --- | --- |
| Capability samples | **Upload + download**: `upload` then `download` and compare payload; **Head**: read metadata after upload; **Delete**: object is gone (`exists=false`) |
| Runner | `StorageDemoRunner`; disable with `quickstart.store.demo.enabled=false` |
| Minimal config | `peach.storage.enabled=true`, `primary=local`, `providers.local.type=LOCAL`, `root-path`, `bucket-name` |
| Prerequisites | Local filesystem only |

```yaml
peach:
  storage:
    enabled: true
    primary: local
    providers:
      local:
        type: LOCAL
        bucket-name: quickstart
        root-path: ${java.io.tmpdir}/peach-store-quickstart
        domain: http://localhost/files
quickstart:
  store:
    demo:
      enabled: true
```

```bash
mvn -pl peach-component/peach-storage/peach-store-quickstart -am spring-boot:run
mvn -pl peach-component/peach-storage/peach-store-quickstart -am test
```

## Provider Extension

Adding a provider requires at least:

- Implement `StorageProvider`.
- Implement `StorageProviderFactory`.
- Register the factory in `META-INF/services/com.peach.storage.spi.StorageProviderFactory`.
- Add type constraints in `StorageType` and validation logic.

## Boundaries

- `objectKey` is a business object key, not a local absolute path, and must not contain `..`.
- `copy`, `move`, and `batchDelete` do not guarantee transaction semantics and may partially succeed.
- LOCAL/NAS/SFTP presigned URLs are degraded URLs, not object-storage access-control credentials.
- Secrets, signed URLs, and frontend upload tokens must not be logged in full.
- Large-file capabilities depend on the selected provider.

## Verification

```bash
mvn -f "peach-component/peach-storage/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
