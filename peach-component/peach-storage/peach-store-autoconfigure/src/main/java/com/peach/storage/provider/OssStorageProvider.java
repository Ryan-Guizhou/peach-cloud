package com.peach.storage.provider;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PolicyConditions;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.MatchMode;
import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.exception.StorageException;
import com.peach.request.FrontendUploadTokenRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.UploadPartRequest;
import com.peach.request.CompleteMultipartUploadRequest;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.AbortMultipartUploadResult;
import com.peach.response.DeleteResult;
import com.peach.response.FrontendUploadTokenResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.enums.StorageResultCode;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.enums.StorageType;
import com.peach.storage.spi.StorageProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 阿里云 OSS 存储实现。
 *
 * <p>该实现使用阿里云官方 {@code aliyun-sdk-oss}，不通过通用 S3 协议适配。
 * 配置中必须提供 endpoint、accessKey、secretKey 和 bucketName。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 14:52
 */
public class OssStorageProvider implements StorageProvider {

    private final StorageProperties.StorageProvider config;

    private final OSS client;

    private final String domain;

    private final boolean publicRead;

    public OssStorageProvider(StorageProperties.StorageProvider config) {
        this.config = config;
        this.client = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKey(), config.getSecretKey());
        this.domain = config.getDomain();
        this.publicRead = config.isPublicRead();
    }

    @Override
    public String bucketName() {
        return bucketName(config);
    }

    @Override
    public StorageType storageType() {
        return StorageType.OSS;
    }

    @Override
    public String name() {
        return name(config);
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        try {
            return client.doesObjectExist(bucketName(config, bucketName), buildObjectKey(config, objectKey));
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to check OSS object exists: " + objectKey, ex);
        }
    }

    @Override
    public UploadResult upload(UploadObjectRequest request) {
        if (request.getContent() == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload content must not be null");
        }
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            ObjectMetadata metadata = buildMetadata(request);
            PutObjectResult result;
            try (InputStream inputStream = request.getContent().read()) {
                result = client.putObject(new PutObjectRequest(bucketName(config, request.getBucketName()), objectKey,
                        inputStream, metadata));
            } finally {
                request.getContent().close();
            }
            if (publicRead || request.isPublicRead()) {
                client.setObjectAcl(bucketName(config, request.getBucketName()), objectKey,
                        CannedAccessControlList.PublicRead);
            }
            return new UploadResult(name(), bucketName(config, request.getBucketName()), rawObjectKey(request.getObjectKey()),
                    request.getContent().length(), publicUrl(domain, objectKey), result.getETag());
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to upload OSS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            OSSObject object = client.getObject(new GetObjectRequest(bucketName(config, request.getBucketName()), objectKey));
            return object.getObjectContent();
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to download OSS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.deleteObject(bucketName(config, request.getBucketName()), objectKey);
            return new DeleteResult(name(), bucketName(config, request.getBucketName()), rawObjectKey(request.getObjectKey()),
                    true);
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to delete OSS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.doesBucketExist(bucketName(config, bucketName));
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to check OSS bucket exists: " + bucketName, ex);
        }
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            ObjectMetadata metadata = client.getObjectMetadata(bucketName(config, request.getBucketName()), objectKey);
            return ObjectInfo.builder()
                    .providerName(name())
                    .bucketName(bucketName(config, request.getBucketName()))
                    .objectKey(rawObjectKey(request.getObjectKey()))
                    .size(metadata.getContentLength())
                    .contentType(metadata.getContentType())
                    .etag(metadata.getETag())
                    .lastModified(toInstant(metadata.getLastModified()))
                    .metadata(metadata.getUserMetadata())
                    .build();
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to stat OSS object: " + request.getObjectKey(), ex);
        }
    }


    @Override
    public ListObjectsResult list(com.peach.request.ListObjectsRequest request) {
        try {
            ListObjectsRequest listRequest = new ListObjectsRequest(bucketName(config, request.getBucketName()));
            Optional.ofNullable(request.getPrefix())
                            .ifPresent(prefix -> listRequest.setPrefix(buildObjectKey(config,prefix)));
            Optional.ofNullable(request.getContinuationToken())
                            .ifPresent(continuationToken -> listRequest.setMarker(continuationToken));
            Optional.ofNullable(resolveDelimiter(request))
                            .ifPresent(delimiter -> listRequest.setDelimiter(delimiter));
            listRequest.setMaxKeys(request.getMaxKeys());
            ObjectListing listing = client.listObjects(listRequest);
            List<ObjectInfo> items = new ArrayList<>();
            for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                items.add(ObjectInfo.builder()
                        .providerName(name())
                        .bucketName(summary.getBucketName())
                        .objectKey(businessObjectKey(config, summary.getKey()))
                        .size(summary.getSize())
                        .etag(summary.getETag())
                        .lastModified(toInstant(summary.getLastModified()))
                        .build());
            }
            return buildListResult(name(), bucketName(config, request.getBucketName()), request.getPrefix(), items,
                    listing.getNextMarker(), listing.isTruncated(), listing.getCommonPrefixes());
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to list OSS objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        Date expires = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            String url = client.generatePresignedUrl(bucketName(config, request.getBucketName()), objectKey, expires)
                    .toString();
            return new PresignedUrlResult(name(), bucketName(config, request.getBucketName()), rawObjectKey(request.getObjectKey()),
                    url, expires.toInstant());
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to generate OSS presigned url: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expiration = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            PolicyConditions policyConditions = new PolicyConditions();
            policyConditions.addConditionItem(MatchMode.Exact, PolicyConditions.COND_KEY, actualObjectKey);
            policyConditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, request.getMaxSize());
            String postPolicy = client.generatePostPolicy(expiration, policyConditions);
            String encodedPolicy = BinaryUtil.toBase64String(postPolicy.getBytes("UTF-8"));
            String signature = client.calculatePostSignature(postPolicy);
            return new FrontendUploadTokenResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    resolveOssPostHost(actualBucket), config.getAccessKey(), encodedPolicy, signature,
                    expiration.toInstant());
        } catch (Exception ex) {
            throw toStorageException("Failed to create OSS frontend upload token: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public com.peach.response.InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.aliyun.oss.model.InitiateMultipartUploadRequest ossRequest =
                    new com.aliyun.oss.model.InitiateMultipartUploadRequest(actualBucket, actualObjectKey);
            ossRequest.setObjectMetadata(buildMetadata(request));
            InitiateMultipartUploadResult result = client.initiateMultipartUpload(ossRequest);
            if (publicRead || request.isPublicRead()) {
                client.setObjectAcl(actualBucket, actualObjectKey, CannedAccessControlList.PublicRead);
            }
            return new com.peach.response.InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), result.getUploadId());
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to initiate OSS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expiration = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            GeneratePresignedUrlRequest presignedUrlRequest =
                    new GeneratePresignedUrlRequest(actualBucket, actualObjectKey, com.aliyun.oss.HttpMethod.PUT);
            presignedUrlRequest.setExpiration(expiration);
            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put("uploadId", request.getUploadId());
            queryParameters.put("partNumber", String.valueOf(request.getPartNumber()));
            presignedUrlRequest.setQueryParameter(queryParameters);
            presignedUrlRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            String url = client.generatePresignedUrl(presignedUrlRequest).toString();
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), url, expiration.toInstant());
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to prepare OSS multipart upload part: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public com.peach.response.CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            List<PartETag> partETags = new ArrayList<>();
            for (CompleteMultipartUploadRequest.Part part : request.getParts()) {
                partETags.add(new PartETag(part.getPartNumber(), part.getETag()));
            }
            com.aliyun.oss.model.CompleteMultipartUploadRequest ossRequest =
                    new com.aliyun.oss.model.CompleteMultipartUploadRequest(actualBucket,
                            actualObjectKey, request.getUploadId(), partETags);
            CompleteMultipartUploadResult result = client.completeMultipartUpload(ossRequest);
            return new com.peach.response.CompleteMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), request.getUploadId(), result.getETag(),
                    result.getLocation());
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to complete OSS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.abortMultipartUpload(new com.aliyun.oss.model.AbortMultipartUploadRequest(actualBucket, actualObjectKey,
                    request.getUploadId()));
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to abort OSS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public void setPublicReadAcl(String objectKey) {
        try {
            client.setObjectAcl(bucketName(), buildObjectKey(config, objectKey), CannedAccessControlList.PublicRead);
        } catch (OSSException | ClientException ex) {
            throw toStorageException("Failed to set OSS public read acl: " + objectKey, ex);
        }
    }

    @Override
    public Set<StorageCapability> capabilities() {
        Set<StorageCapability> capabilities = baseCapabilities(true);
        capabilities.add(StorageCapability.FRONTEND_UPLOAD_TOKEN);
        capabilities.add(StorageCapability.PRESIGNED_PUT_URL);
        capabilities.add(StorageCapability.MULTIPART_UPLOAD);
        return capabilities;
    }

    @Override
    public void close() {
        client.shutdown();
    }

    /**
     * 为普通对象上传请求构建元数据。
     *
     * <p>从 {@link UploadObjectRequest} 中提取内容长度、内容类型和自定义元数据，
     * 并转换为阿里云 OSS SDK 所需的 {@link ObjectMetadata} 对象。</p>
     *
     * @param request 上传对象请求，包含内容长度、内容类型和自定义元数据
     * @return 配置完成的 OSS 对象元数据
     * @throws Exception 获取内容长度时发生异常
     */
    private ObjectMetadata buildMetadata(UploadObjectRequest request) throws Exception {
        ObjectMetadata metadata = buildBaseMetadata(request.getContentType(), request.getMetadata());
        long length = request.getContent().length();
        if (length >= 0) {
            metadata.setContentLength(length);
        }
        return metadata;
    }

    /**
     * 为分片上传初始化请求构建元数据。
     *
     * <p>从 {@link InitiateMultipartUploadRequest} 中提取内容类型和自定义元数据，
     * 并转换为阿里云 OSS SDK 所需的 {@link ObjectMetadata} 对象。分片上传不需要预设内容长度。</p>
     *
     * @param request 分片上传初始化请求，包含内容类型和自定义元数据
     * @return 配置完成的 OSS 对象元数据
     */
    private ObjectMetadata buildMetadata(InitiateMultipartUploadRequest request) {
        return buildBaseMetadata(request.getContentType(), request.getMetadata());
    }

    /**
     * 构建基础对象元数据。
     *
     * <p>提取公共的元数据构建逻辑：设置内容类型（非空时）和添加所有自定义元数据。</p>
     *
     * @param contentType 内容类型，为空或空白字符串时不设置
     * @param userMetadata 自定义元数据键值对
     * @return 配置完成的 OSS 对象元数据
     */
    private ObjectMetadata buildBaseMetadata(String contentType, Map<String, String> userMetadata) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (StringUtils.isNotBlank(contentType)) {
            metadata.setContentType(contentType);
        }
        if (userMetadata != null) {
            userMetadata.forEach(metadata::addUserMetadata);
        }
        return metadata;
    }

    /**
     * 解析 OSS Post 请求的 Host 地址。
     *
     * <p>根据配置的 endpoint 和 bucket 名称，构建符合 OSS 规范的 Post 上传地址。
     * 格式为：{@code scheme://bucket.host}。如果 endpoint 未指定 scheme，默认使用 HTTPS。</p>
     *
     * @param actualBucket 实际使用的 bucket 名称
     * @return 完整的 OSS Post 请求 Host 地址
     */
    private String resolveOssPostHost(String actualBucket) {
        URI uri = URI.create(normalizeEndpoint(config.getEndpoint()));
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("https");
        String host = StringUtils.isNotBlank(uri.getHost()) ? uri.getHost() : uri.getAuthority();
        return scheme + "://" + actualBucket + "." + host;
    }

}
