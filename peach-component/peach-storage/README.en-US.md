# peach-storage

English | [中文](README.md)

## Purpose

`peach-storage` is a unified storage starter. It exposes `StorageTemplate` to hide differences between local files, NAS/SFTP, OSS, OBS, S3, MinIO, COS, BOS, Ceph, and other providers. It supports upload, download, delete, list, copy, move, presigned URLs, frontend direct upload, and multipart upload.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-store-autoconfigure` | Core APIs, request/response models, provider SPI, auto-configuration, and defaults |
| `peach-store-starter` | Starter exposed to business modules |
| `peach-store-example` | Minimal example project |

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
