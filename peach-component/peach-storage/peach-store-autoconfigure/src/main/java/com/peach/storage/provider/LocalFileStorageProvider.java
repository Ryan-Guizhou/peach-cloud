package com.peach.storage.provider;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageResultCode;
import com.peach.enums.StorageType;
import com.peach.exception.StorageException;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.BatchDeleteResult;
import com.peach.response.CopyResult;
import com.peach.response.DeleteResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.MoveResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadResult;
import com.peach.storage.spi.StorageProvider;
import com.peach.util.StoragePathUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 本地文件系统存储实现。
 *
 * <p>objectKey 会被解析到配置的 rootPath 下，并阻止路径越界。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
@Slf4j
public class LocalFileStorageProvider implements StorageProvider {

    /**
     * 当前 provider 配置。
     */
    private final StorageProperties.StorageProvider config;

    /**
     * 本地存储根目录。
     */
    private final Path rootPath;

    /**
     * 自定义访问域名。
     */
    private final String domain;

    /**
     * 创建本地文件系统存储 provider。
     *
     * @param config provider 配置
     */
    public LocalFileStorageProvider(StorageProperties.StorageProvider config) {
        this.config = config;
        this.rootPath = StoragePathUtil.parseLocalPath(config.getRootPath()).toAbsolutePath().normalize();
        this.domain = config.getDomain();
        try {
            Files.createDirectories(rootPath);
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to create storage root path: " + rootPath, ex);
        }
    }

    @Override
    public String bucketName() {
        return bucketName(config);
    }

    @Override
    public StorageType storageType() {
        return config.getType();
    }

    @Override
    public String name() {
        return name(config);
    }

    @Override
    public boolean isBucketless() {
        return true;
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        return Files.exists(resolve(objectKey)) && Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public boolean bucketExists(String bucketName) {
        return Files.exists(rootPath) && Files.isDirectory(rootPath);
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        Path file = resolve(request.getObjectKey());
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new StorageException(StorageResultCode.OBJECT_NOT_FOUND,
                    "Object not found: " + request.getObjectKey());
        }
        try {
            return ObjectInfo.builder()
                    .providerName(name())
                    .bucketName(bucketName(config, request.getBucketName()))
                    .objectKey(rawObjectKey(request.getObjectKey()))
                    .size(Files.size(file))
                    .contentType(Files.probeContentType(file))
                    .lastModified(Files.getLastModifiedTime(file).toInstant())
                    .build();
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to stat object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadResult upload(UploadObjectRequest request) {
        if (request.getContent() == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload content must not be null");
        }
        String objectKey = buildObjectKey(config, request.getObjectKey());
        Path target = resolveObjectKey(objectKey);
        try {
            Path parent = target.getParent();
            if (!Objects.isNull(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = request.getContent().read()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                request.getContent().close();
            }
            long size = Files.size(target);
            return new UploadResult(name(), bucketName(config, request.getBucketName()),
                    rawObjectKey(request.getObjectKey()), size, publicUrl(domain, objectKey), null);
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to upload object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        Path file = resolve(request.getObjectKey());
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new StorageException(StorageResultCode.OBJECT_NOT_FOUND,
                    "Object not found: " + request.getObjectKey());
        }
        try {
            return Files.newInputStream(file);
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to download object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        Path file = resolve(request.getObjectKey());
        try {
            boolean deleted = Files.deleteIfExists(file);
            return new DeleteResult(name(), bucketName(config, request.getBucketName()),
                    rawObjectKey(request.getObjectKey()), deleted);
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to delete object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public BatchDeleteResult batchDelete(BatchDeleteObjectsRequest request) {
        int deletedCount = 0;
        for (String objectKey : request.getObjectKeys()) {
            if (delete(DeleteObjectRequest.builder()
                    .bucketName(request.getBucketName())
                    .objectKey(objectKey)
                    .build()).isDeleted()) {
                deletedCount++;
            }
        }
        return new BatchDeleteResult(name(), bucketName(config, request.getBucketName()),
                request.getObjectKeys(), deletedCount);
    }

    @Override
    public CopyResult copy(CopyObjectRequest request) {
        Path source = resolve(request.getSourceObjectKey());
        Path target = resolve(request.getTargetObjectKey());
        validateTransferBounds(source, target, request.getSourceObjectKey(), request.getTargetObjectKey());
        try {
            ensureSourceExists(source, request.getSourceObjectKey());
            ensureTargetParent(target);
            if (Files.isDirectory(source)) {
                if (!request.isRecursive()) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Recursive flag must be true when copying a directory: " + request.getSourceObjectKey());
                }
                copyDirectory(source, target, request.isOverwrite());
            } else {
                copyFile(source, target, request.isOverwrite());
            }
            return new CopyResult(name(),
                    bucketName(config, request.getSourceBucketName()), rawObjectKey(request.getSourceObjectKey()),
                    bucketName(config, request.getTargetBucketName()), rawObjectKey(request.getTargetObjectKey()), true);
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to copy object: " + request.getSourceObjectKey(), ex);
        }
    }

    @Override
    public MoveResult move(MoveObjectRequest request) {
        Path source = resolve(request.getSourceObjectKey());
        Path target = resolve(request.getTargetObjectKey());
        validateTransferBounds(source, target, request.getSourceObjectKey(), request.getTargetObjectKey());
        try {
            ensureSourceExists(source, request.getSourceObjectKey());
            ensureTargetParent(target);
            if (Files.isDirectory(source) && !request.isRecursive()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST,
                        "Recursive flag must be true when moving a directory: " + request.getSourceObjectKey());
            }
            if (Files.exists(target) && !request.isOverwrite()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST,
                        "Target object already exists: " + request.getTargetObjectKey());
            }
            if (Files.exists(target)) {
                deletePath(target);
            }
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return new MoveResult(name(),
                    bucketName(config, request.getSourceBucketName()), rawObjectKey(request.getSourceObjectKey()),
                    bucketName(config, request.getTargetBucketName()), rawObjectKey(request.getTargetObjectKey()), true);
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to move object: " + request.getSourceObjectKey(), ex);
        }
    }

    @Override
    public ListObjectsResult list(ListObjectsRequest request) {
        String prefix = StringUtils.isBlank(request.getPrefix()) ? null : buildObjectKey(config, request.getPrefix());
        Path startPath = prefix == null ? rootPath : resolveObjectKey(prefix);
        List<ObjectInfo> objects = new ArrayList<>();
        if (!Files.exists(startPath)) {
            return buildListResult(name(), bucketName(config, request.getBucketName()),
                    request.getPrefix(), objects, null, false, noCommonPrefixes());
        }
        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> toObjectInfo(path, request.getBucketName()))
                    .sorted(Comparator.comparing(ObjectInfo::getObjectKey))
                    .forEach(objects::add);
            return slicePage(request, objects);
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to list objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        String url = publicUrl(domain, objectKey);
        if (StringUtils.isNotBlank(url)) {
            URI uri = resolveObjectKey(objectKey).toUri();
            url = uri.toString();
        }
        return new PresignedUrlResult(name(), bucketName(config, request.getBucketName()),
                rawObjectKey(request.getObjectKey()), url, Instant.now().plusSeconds(request.getExpireSeconds()));
    }

    @Override
    public Set<StorageCapability> capabilities() {
        return EnumSet.copyOf(baseCapabilities(false));
    }

    /**
     * 将本地文件路径转换为统一的对象信息。
     *
     * @param path       本地文件路径
     * @param bucketName 请求指定的 bucket 名称
     * @return 对象信息
     */
    private ObjectInfo toObjectInfo(Path path, String bucketName) {
        try {
            String actualObjectKey = StoragePathUtil.toObjectKey(rootPath, path);
            return ObjectInfo.builder()
                    .providerName(name())
                    .bucketName(bucketName(config, bucketName))
                    .objectKey(businessObjectKey(config, actualObjectKey))
                    .size(Files.size(path))
                    .contentType(Files.probeContentType(path))
                    .lastModified(Files.getLastModifiedTime(path).toInstant())
                    .build();
        } catch (Exception ex) {
            log.warn("Failed to read object info, path={}", path, ex);
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to read object info: " + path, ex);
        }
    }

    /**
     * 将请求对象 key 解析为本地文件路径。
     *
     * @param requestObjectKey 请求对象 key
     * @return 本地文件路径
     */
    private Path resolve(String requestObjectKey) {
        return resolveObjectKey(buildObjectKey(config, requestObjectKey));
    }

    /**
     * 将标准对象 key 解析为本地文件路径。
     *
     * @param objectKey 标准对象 key
     * @return 本地文件路径
     */
    private Path resolveObjectKey(String objectKey) {
        return StoragePathUtil.resolveLocalPath(rootPath, objectKey);
    }

    /**
     * 根据 continuationToken 和 maxKeys 截取对象分页结果。
     *
     * @param request    列表查询请求
     * @param allObjects 全量对象列表
     * @return 分页后的对象列表结果
     */
    private ListObjectsResult slicePage(ListObjectsRequest request, List<ObjectInfo> allObjects) {
        int startIndex = findPageStartIndex(allObjects, request.getContinuationToken());
        int endIndex = Math.min(startIndex + request.getMaxKeys(), allObjects.size());
        List<ObjectInfo> pageItems = new ArrayList<>(allObjects.subList(startIndex, endIndex));

        boolean truncated = endIndex < allObjects.size();
        String nextContinuationToken = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1).getObjectKey();

        log.debug("Slice object page, bucket={}, prefix={}, maxKeys={}, returned={}, truncated={}",
                bucketName(config, request.getBucketName()), request.getPrefix(), request.getMaxKeys(),
                pageItems.size(), truncated);

        return buildListResult(name(), bucketName(config, request.getBucketName()),
                request.getPrefix(), pageItems, nextContinuationToken, truncated, noCommonPrefixes());
    }

    /**
     * 确认源对象存在。
     *
     * @param source    源文件路径
     * @param objectKey 源对象 key
     */
    private void ensureSourceExists(Path source, String objectKey) {
        if (!Files.exists(source)) {
            log.warn("Source object not found, objectKey={}, path={}", objectKey, source);
            throw new StorageException(StorageResultCode.OBJECT_NOT_FOUND, "Object not found: " + objectKey);
        }
    }

    /**
     * 确保目标路径父目录存在。
     *
     * @param target 目标路径
     * @throws Exception 创建目录失败时抛出
     */
    private void ensureTargetParent(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 校验复制或移动操作的源路径与目标路径边界。
     *
     * @param source          源路径
     * @param target          目标路径
     * @param sourceObjectKey 源对象 key
     * @param targetObjectKey 目标对象 key
     */
    private void validateTransferBounds(Path source, Path target, String sourceObjectKey, String targetObjectKey) {
        if (source.equals(target)) {
            log.warn("Invalid transfer path, source and target are same, objectKey={}", sourceObjectKey);
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Source object key and target object key must not be the same");
        }
        if (target.startsWith(source)) {
            log.warn("Invalid transfer path, target inside source, source={}, target={}", sourceObjectKey, targetObjectKey);
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Target object must not be inside source path: " + targetObjectKey);
        }
        if (source.startsWith(target)) {
            log.warn("Invalid transfer path, target is parent of source, source={}, target={}",
                    sourceObjectKey, targetObjectKey);
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Target object must not be parent of source path: " + sourceObjectKey);
        }
    }

    /**
     * 复制单个文件。
     *
     * @param source    源文件路径
     * @param target    目标文件路径
     * @param overwrite 是否覆盖已有文件
     * @throws Exception 复制失败时抛出
     */
    private void copyFile(Path source, Path target, boolean overwrite) throws IOException {
        if (Files.exists(target) && !overwrite) {
            log.warn("Target object already exists, target={}, overwrite={}", target, overwrite);
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Target object already exists: " + target);
        }
        if (overwrite) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(source, target);
        }
    }

    /**
     * 递归复制目录。
     *
     * @param source    源目录路径
     * @param target    目标目录路径
     * @param overwrite 是否覆盖已有文件
     * @throws Exception 复制失败时抛出
     */
    private void copyDirectory(Path source, Path target, boolean overwrite) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path currentTarget = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(currentTarget);
                } else {
                    Path parent = currentTarget.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    copyFile(path, currentTarget, overwrite);
                }
            }
        }
    }

    /**
     * 删除文件或目录。
     *
     * @param path 文件或目录路径
     * @throws Exception 删除失败时抛出
     */
    private void deletePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path current : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }

    private static int findPageStartIndex(List<ObjectInfo> allObjects, String continuationToken) {
        if (continuationToken == null || continuationToken.isBlank()) {
            return 0;
        }
        for (int i = 0; i < allObjects.size(); i++) {
            if (allObjects.get(i).getObjectKey().compareTo(continuationToken) > 0) {
                return i;
            }
        }
        return allObjects.size();
    }
}
