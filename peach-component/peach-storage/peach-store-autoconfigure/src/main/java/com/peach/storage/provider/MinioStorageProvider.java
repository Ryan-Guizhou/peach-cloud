package com.peach.storage.provider;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageResultCode;
import com.peach.enums.StorageType;
import com.peach.exception.StorageException;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.CompleteMultipartUploadRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.FrontendUploadTokenRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadPartRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.AbortMultipartUploadResult;
import com.peach.response.CompleteMultipartUploadResult;
import com.peach.response.DeleteResult;
import com.peach.response.FrontendUploadTokenResult;
import com.peach.response.InitiateMultipartUploadResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.storage.spi.StorageProvider;
import io.minio.BucketExistsArgs;
import io.minio.GetBucketPolicyArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储实现。
 *
 * <p>该实现基于 MinIO 原生 Java SDK，对外保留 `MINIO` 类型，同时补齐可直接通过
 * MinIO/S3 兼容协议实现的前端直传、分片上传、分页列表和预签名上传能力。</p>
 */
public class MinioStorageProvider implements StorageProvider {

    private static final long UNKNOWN_LENGTH_PART_SIZE = 10L * 1024L * 1024L;

    private final StorageProperties.StorageProvider config;

    private final MinioClient client;

    private final AdvancedMinioAsyncClient multipartClient;

    private final String domain;

    private final boolean publicRead;

    public MinioStorageProvider(StorageProperties.StorageProvider config) {
        this.config = config;
        this.client = createClient(config);
        this.multipartClient = new AdvancedMinioAsyncClient(createAsyncClient(config));
        this.domain = config.getDomain();
        this.publicRead = config.isPublicRead();
    }

    @Override
    public String bucketName() {
        return bucketName(config);
    }

    @Override
    public StorageType storageType() {
        return StorageType.MINIO;
    }

    @Override
    public String name() {
        return name(config);
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        String actualBucket = bucketName(config, bucketName);
        String actualObjectKey = buildObjectKey(config, objectKey);
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .build());
            return true;
        } catch (Exception ex) {
            if (isNotFound(ex)) {
                return false;
            }
            throw toStorageException("Failed to " + "check MinIO object exists: " + objectKey, ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName(config, bucketName))
                    .build());
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "check MinIO bucket exists: " + bucketName, ex);
        }
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            StatObjectResponse response = client.statObject(StatObjectArgs.builder()
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .build());
            return ObjectInfo.builder()
                    .providerName(name())
                    .bucketName(actualBucket)
                    .objectKey(rawObjectKey(request.getObjectKey()))
                    .size(response.size())
                    .contentType(response.contentType())
                    .etag(response.etag())
                    .lastModified(response.lastModified() == null ? null : response.lastModified().toInstant())
                    .metadata(response.userMetadata())
                    .build();
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "stat MinIO object: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadResult upload(UploadObjectRequest request) {
        if (request.getContent() == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload content must not be null");
        }
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            long contentLength = request.getContent().length();
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(actualBucket)
                    .object(actualObjectKey);
            Optional.ofNullable(request.getContentType())
                    .ifPresent(contentType -> builder.contentType(contentType));
            Optional.ofNullable(request.getMetadata())
                    .ifPresent(metadata -> builder.userMetadata(metadata));
            try (InputStream inputStream = request.getContent().read()) {
                if (contentLength >= 0) {
                    builder.stream(inputStream, contentLength, -1);
                } else {
                    builder.stream(inputStream, -1, UNKNOWN_LENGTH_PART_SIZE);
                }
                String etag = client.putObject(builder.build()).etag();
                if (contentLength < 0) {
                    contentLength = client.statObject(StatObjectArgs.builder()
                            .bucket(actualBucket)
                            .object(actualObjectKey)
                            .build()).size();
                }
                if (publicRead || request.isPublicRead()) {
                    ensurePublicReadPolicy(actualBucket, actualObjectKey);
                }
                return new UploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                        contentLength, publicUrl(domain, actualObjectKey), etag);
            } finally {
                request.getContent().close();
            }
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "upload MinIO object: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .build());
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "download MinIO object: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .build());
            return new DeleteResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), true);
        } catch (Exception ex) {
            if (isNotFound(ex)) {
                return new DeleteResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), false);
            }
            throw toStorageException("Failed to " + "delete MinIO object: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public ListObjectsResult list(ListObjectsRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualPrefix = StringUtils.isBlank(request.getPrefix()) ? null : buildObjectKey(config, request.getPrefix());
        try {
            List<ObjectInfo> allObjects = new ArrayList<ObjectInfo>();
            LinkedHashSet<String> commonPrefixes = new LinkedHashSet<String>();
            ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                    .bucket(actualBucket)
                    .prefix(actualPrefix)
                    .maxKeys(Math.max(request.getMaxKeys(), 1))
                    .recursive(request.isRecursive());
            Optional.ofNullable(request.getContinuationToken())
                            .ifPresent(continuationToken -> builder.continuationToken(continuationToken));
            builder.delimiter(resolveDelimiter(request));
            Iterable<Result<Item>> results = client.listObjects(builder.build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item == null) {
                    continue;
                }
                if (item.isDir()) {
                    commonPrefixes.add(businessObjectKey(config, trimTrailingSlash(item.objectName())));
                    continue;
                }
                allObjects.add(ObjectInfo.builder()
                        .providerName(name())
                        .bucketName(actualBucket)
                        .objectKey(businessObjectKey(config, item.objectName()))
                        .size(item.size())
                        .etag(item.etag())
                        .lastModified(item.lastModified() == null ? null : item.lastModified().toInstant())
                        .build());
            }
            allObjects.sort(Comparator.comparing(ObjectInfo::getObjectKey));
            return slicePage(actualBucket, request, allObjects, new ArrayList<String>(commonPrefixes));
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "list MinIO objects by prefix: "
                    + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        String customUrl = publicUrl(domain, actualObjectKey);
        if (customUrl != null) {
            return new PresignedUrlResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    customUrl, Instant.now().plusSeconds(request.getExpireSeconds()));
        }
        try {
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .expiry((int) request.getExpireSeconds(), TimeUnit.SECONDS)
                    .build());
            return new PresignedUrlResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), url,
                    Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "generate MinIO presigned url: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            PostPolicy policy = new PostPolicy(actualBucket,
                    ZonedDateTime.now().plusSeconds(request.getExpireSeconds()));
            policy.addEqualsCondition("key", actualObjectKey);
            policy.addContentLengthRangeCondition(0, request.getMaxSize());
            Map<String, String> formData = client.getPresignedPostFormData(policy);
            String host = normalizeEndpoint(config.getEndpoint());
            if (!host.endsWith("/")) {
                host = host + "/";
            }
            host = host + actualBucket;
            return new FrontendUploadTokenResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    host,
                    config.getAccessKey(),
                    formData.get("policy"),
                    firstText(formData.get("x-amz-signature"), formData.get("signature")),
                    Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "create MinIO frontend upload token: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            String uploadId = multipartClient.createMultipartUpload(actualBucket, config.getRegion(),
                    actualObjectKey);
            if (publicRead || request.isPublicRead()) {
                ensurePublicReadPolicy(actualBucket, actualObjectKey);
            }
            return new InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), uploadId);
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "initiate MinIO multipart upload: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(actualBucket)
                    .object(actualObjectKey)
                    .expiry((int) request.getExpireSeconds(), TimeUnit.SECONDS)
                    .extraQueryParams(partUploadQuery(request))
                    .build());
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), url,
                    Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "prepare MinIO multipart upload part: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            String etag = multipartClient.completeMultipartUpload(actualBucket, config.getRegion(),
                    actualObjectKey, request.getUploadId(), toMinioParts(request.getParts()));
            String url = publicUrl(domain, actualObjectKey);
            return new CompleteMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), etag, url);
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "complete MinIO multipart upload: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            multipartClient.abortMultipartUpload(actualBucket, config.getRegion(),
                    actualObjectKey, request.getUploadId());
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to " + "abort MinIO multipart upload: "
                    + request.getObjectKey(), ex);
        }
    }

    @Override
    public Set<StorageCapability> capabilities() {
        Set<StorageCapability> capabilities = EnumSet.copyOf(baseCapabilities(false));
        capabilities.add(StorageCapability.PRESIGNED_PUT_URL);
        capabilities.add(StorageCapability.MULTIPART_UPLOAD);
        capabilities.add(StorageCapability.FRONTEND_UPLOAD_TOKEN);
        return capabilities;
    }

    /**
     * 对对象列表进行分页处理。
     *
     * <p>根据 continuation token 和 maxKeys 参数，从完整的对象列表中截取指定页面的数据。
     * 如果还有更多数据，则设置 nextContinuationToken 和 truncated 标志。</p>
     *
     * @param actualBucket 实际使用的 bucket 名称
     * @param request 列表请求，包含分页参数（continuationToken 和 maxKeys）
     * @param allObjects 已排序的完整对象列表
     * @param commonPrefixes 公共前缀列表（目录）
     * @return 分页后的列表结果
     */
    private ListObjectsResult slicePage(String actualBucket, ListObjectsRequest request, List<ObjectInfo> allObjects,
                                        List<String> commonPrefixes) {
        List<ObjectInfo> pageItems = new ArrayList<>();
        String continuationToken = request.getContinuationToken();
        boolean started = StringUtils.isBlank(continuationToken);

        for (ObjectInfo object : allObjects) {
            if (!started) {
                if (object.getObjectKey().compareTo(continuationToken) > 0) {
                    started = true;
                } else {
                    continue;
                }
            }
            if (pageItems.size() >= request.getMaxKeys()) {
                break;
            }
            pageItems.add(object);
        }

        boolean truncated = false;
        String nextContinuationToken = null;
        if (!pageItems.isEmpty()) {
            int lastIndex = allObjects.indexOf(pageItems.get(pageItems.size() - 1));
            if (lastIndex >= 0 && lastIndex < allObjects.size() - 1) {
                truncated = true;
                nextContinuationToken = pageItems.get(pageItems.size() - 1).getObjectKey();
            }
        }

        return buildListResult(name(), actualBucket, request.getPrefix(),
                pageItems, nextContinuationToken, truncated, commonPrefixes);
    }

    /**
     * 将分片信息列表转换为 MinIO SDK 所需的 Part 数组。
     *
     * @param parts 分片信息列表，包含每个分片的分片号和 ETag
     * @return MinIO Part 数组
     */
    private io.minio.messages.Part[] toMinioParts(List<CompleteMultipartUploadRequest.Part> parts) {
        return parts.stream()
                .map(part -> new io.minio.messages.Part(part.getPartNumber(), part.getETag()))
                .toArray(io.minio.messages.Part[]::new);
    }

    /**
     * 构建分片上传的查询参数。
     *
     * <p>生成用于预签名 URL 的查询参数，包含 uploadId 和 partNumber。</p>
     *
     * @param request 分片上传请求，包含 uploadId 和 partNumber
     * @return 查询参数 Map，保持插入顺序
     */
    private Map<String, String> partUploadQuery(UploadPartRequest request) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("uploadId", request.getUploadId());
        query.put("partNumber", String.valueOf(request.getPartNumber()));
        return query;
    }

    /**
     * 移除字符串末尾的斜杠。
     *
     * @param value 输入字符串
     * @return 移除末尾斜杠后的字符串，如果输入为 null 或空则原样返回
     */
    private String trimTrailingSlash(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 确保指定对象具有公开读取权限。
     *
     * <p>检查 bucket 策略中是否已包含指定对象的公开读取权限，如果没有则添加相应的策略声明。
     * 使用 AWS S3 策略语法为对象授予 {@code s3:GetObject} 权限。</p>
     *
     * @param bucketName bucket 名称
     * @param actualObjectKey 对象的实际 key
     * @throws Exception 获取或设置策略时发生异常
     */
    private void ensurePublicReadPolicy(String bucketName, String actualObjectKey) throws Exception {
        String policy = client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());
        String resource = "arn:aws:s3:::" + bucketName + "/" + actualObjectKey;

        if (policy != null && policy.contains(resource)) {
            return;
        }

        String statement = "{"
                + "\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"" + resource + "\"]"
                + "}";

        String updated = StringUtils.isBlank(policy)
                ? "{\"Version\":\"2012-10-17\",\"Statement\":[" + statement + "]}"
                : appendStatement(policy, statement);

        client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(updated).build());
    }

    /**
     * 向现有策略中追加新的策略声明。
     *
     * <p>解析现有的 JSON 策略文档，在 Statement 数组中追加新的声明。
     * 如果策略格式不正确（缺少 Statement 数组），则返回一个全新的策略文档。</p>
     *
     * @param policy 现有的策略 JSON 文档
     * @param statement 要追加的策略声明 JSON
     * @return 更新后的策略 JSON 文档
     */
    private String appendStatement(String policy, String statement) {
        String value = StringUtils.trimToEmpty(policy);
        int index = value.lastIndexOf(']');
        if (index < 0) {
            return "{\"Version\":\"2012-10-17\",\"Statement\":[" + statement + "]}";
        }

        int openIndex = value.lastIndexOf('[', index);
        if (openIndex < 0) {
            return "{\"Version\":\"2012-10-17\",\"Statement\":[" + statement + "]}";
        }

        String prefix = value.substring(0, index);
        String suffix = value.substring(index);

        return prefix.charAt(prefix.length() - 1) == '['
                ? prefix + statement + suffix
                : prefix + "," + statement + suffix;
    }

    /**
     * 创建 MinIO 同步客户端。
     *
     * <p>根据配置信息构建 {@link MinioClient}，设置 endpoint、credentials 和 region。</p>
     *
     * @param config 存储提供者配置，包含 endpoint、accessKey、secretKey 和 region
     * @return 配置完成的 MinIO 客户端实例
     */
    private MinioClient createClient(StorageProperties.StorageProvider config) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(normalizeEndpoint(config.getEndpoint()))
                .credentials(config.getAccessKey(), config.getSecretKey());
        if (StringUtils.isNotBlank(config.getRegion())) {
            builder.region(config.getRegion().trim());
        }
        return builder.build();
    }

    /**
     * 创建 MinIO 异步客户端。
     *
     * <p>根据配置信息构建 {@link MinioAsyncClient}，设置 endpoint、credentials 和 region。
     * 用于分片上传等需要异步操作的场景。</p>
     *
     * @param config 存储提供者配置，包含 endpoint、accessKey、secretKey 和 region
     * @return 配置完成的 MinIO 异步客户端实例
     */
    private MinioAsyncClient createAsyncClient(StorageProperties.StorageProvider config) {
        MinioAsyncClient.Builder builder = MinioAsyncClient.builder()
                .endpoint(normalizeEndpoint(config.getEndpoint()))
                .credentials(config.getAccessKey(), config.getSecretKey());
        if (StringUtils.isNotBlank(config.getRegion())) {
            builder.region(config.getRegion().trim());
        }
        return builder.build();
    }

    /**
     * 判断异常是否为"未找到"错误。
     *
     * <p>检查异常是否为 {@link ErrorResponseException}，并且错误码为
     * NoSuchKey、NoSuchObject 或 NoSuchBucket 之一。</p>
     *
     * @param ex 要检查的异常
     * @return 如果是"未找到"错误返回 true，否则返回 false
     */
    private boolean isNotFound(Exception ex) {
        if (ex instanceof ErrorResponseException) {
            ErrorResponseException error = (ErrorResponseException) ex;
            String code = error.errorResponse() == null ? null : error.errorResponse().code();
            return "NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NoSuchBucket".equals(code);
        }
        return false;
    }

    /**
     * 高级 MinIO 异步客户端，封装分片上传相关操作。
     *
     * <p>继承 {@link MinioAsyncClient}，提供简化的分片上传 API，
     * 隐藏底层 SDK 的复杂参数和返回值处理。</p>
     */
    private static final class AdvancedMinioAsyncClient extends MinioAsyncClient {

        /**
         * 通过现有的 MinioAsyncClient 创建高级客户端。
         *
         * @param client 现有的 MinIO 异步客户端
         */
        private AdvancedMinioAsyncClient(MinioAsyncClient client) {
            super(client);
        }

        /**
         * 创建分片上传任务。
         *
         * @param bucketName bucket 名称
         * @param region 区域
         * @param objectKey 对象 key
         * @return 分片上传任务的 uploadId
         * @throws Exception 创建失败时抛出异常
         */
        private String createMultipartUpload(String bucketName, String region, String objectKey) throws Exception {
            return super.createMultipartUpload(bucketName, region, objectKey, null, null).result().uploadId();
        }

        /**
         * 完成分片上传任务。
         *
         * @param bucketName bucket 名称
         * @param region 区域
         * @param objectKey 对象 key
         * @param uploadId 分片上传任务的 uploadId
         * @param parts 分片信息数组
         * @return 上传完成后的 ETag
         * @throws Exception 完成失败时抛出异常
         */
        private String completeMultipartUpload(String bucketName, String region, String objectKey,
                                               String uploadId, io.minio.messages.Part[] parts) throws Exception {
            return super.completeMultipartUpload(bucketName, region, objectKey, uploadId, parts, null, null).etag();
        }

        /**
         * 中止分片上传任务。
         *
         * @param bucketName bucket 名称
         * @param region 区域
         * @param objectKey 对象 key
         * @param uploadId 分片上传任务的 uploadId
         * @throws Exception 中止失败时抛出异常
         */
        private void abortMultipartUpload(String bucketName, String region, String objectKey, String uploadId)
                throws Exception {
            super.abortMultipartUpload(bucketName, region, objectKey, uploadId, null, null);
        }
    }
}
