package com.peach.storage.provider;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PartEtag;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.exception.StorageException;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.CompleteMultipartUploadRequest.Part;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadPartRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.AbortMultipartUploadResult;
import com.peach.response.DeleteResult;
import com.peach.enums.StorageResultCode;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.enums.StorageType;
import com.peach.storage.spi.StorageProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
/**
 * Obs存储提供者。
 * <p>该实现使用华为云官方 {@code esdk-obs-java}，不通过通用 S3 协议适配。
 * 配置中必须提供 endpoint、accessKey、secretKey 和 bucketName。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
public class ObsStorageProvider implements StorageProvider {

    private final StorageProperties.StorageProvider config;

    private final ObsClient client;

    private final String domain;

    private final boolean publicRead;

    public ObsStorageProvider(StorageProperties.StorageProvider config) {
        this.config = config;
        this.client = new ObsClient(config.getAccessKey(), config.getSecretKey(), config.getEndpoint());
        this.domain = config.getDomain();
        this.publicRead = config.isPublicRead();
    }

    @Override
    public String bucketName() {
        return bucketName(config);
    }

    @Override
    public StorageType storageType() {
        return StorageType.OBS;
    }

    @Override
    public String name() {
        return name(config);
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        try {
            return client.doesObjectExist(bucketName(config, bucketName), buildObjectKey(config, objectKey));
        } catch (ObsException ex) {
            throw toStorageException("Failed to check OBS object exists: " + objectKey, ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.headBucket(bucketName(config, bucketName));
        } catch (ObsException ex) {
            throw toStorageException("Failed to check OBS bucket exists: " + bucketName, ex);
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
                    .etag(metadata.getEtag())
                    .lastModified(toInstant(metadata.getLastModified()))
                    .build();
        } catch (ObsException ex) {
            throw toStorageException("Failed to stat OBS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadResult upload(UploadObjectRequest request) {
        if (request.getContent() == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload content must not be null");
        }
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            PutObjectRequest putRequest = new PutObjectRequest();
            putRequest.setBucketName(bucketName(config, request.getBucketName()));
            putRequest.setObjectKey(objectKey);
            putRequest.setMetadata(buildMetadata(request));
            try (InputStream inputStream = request.getContent().read()) {
                putRequest.setInput(inputStream);
                PutObjectResult result = client.putObject(putRequest);
                if (publicRead || request.isPublicRead()) {
                    setPublicReadAcl(objectKey);
                }
                return new UploadResult(name(), bucketName(config, request.getBucketName()),
                        rawObjectKey(request.getObjectKey()), request.getContent().length(),
                        publicUrl(domain, objectKey), result.getEtag());
            } finally {
                request.getContent().close();
            }
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to upload OBS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            ObsObject object = client.getObject(bucketName(config, request.getBucketName()), objectKey);
            return object.getObjectContent();
        } catch (ObsException ex) {
            throw toStorageException("Failed to download OBS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.deleteObject(bucketName(config, request.getBucketName()), objectKey);
            return new DeleteResult(name(), bucketName(config, request.getBucketName()), rawObjectKey(request.getObjectKey()),
                    true);
        } catch (ObsException ex) {
            throw toStorageException("Failed to delete OBS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public ListObjectsResult list(com.peach.request.ListObjectsRequest request) {
        try {
            ListObjectsRequest listRequest = new ListObjectsRequest(bucketName(config, request.getBucketName()));
            listRequest.setMaxKeys(request.getMaxKeys());
            Optional.ofNullable(request.getPrefix()).ifPresent(listRequest::setPrefix);
            Optional.ofNullable(request.getContinuationToken()).ifPresent(listRequest::setMarker);
            Optional.ofNullable(resolveDelimiter(request)).ifPresent(listRequest::setDelimiter);
            ObjectListing listing = client.listObjects(listRequest);
            List<ObjectInfo> items = new ArrayList<>();
            for (ObsObject object : listing.getObjects()) {
                items.add(ObjectInfo.builder()
                        .providerName(name())
                        .bucketName(bucketName(config, request.getBucketName()))
                        .objectKey(businessObjectKey(config, object.getObjectKey()))
                        .size(object.getMetadata() == null ? -1L : object.getMetadata().getContentLength())
                        .etag(object.getMetadata() == null ? null : object.getMetadata().getEtag())
                        .lastModified(object.getMetadata() == null ? null : toInstant(object.getMetadata().getLastModified()))
                        .build());
            }
            return buildListResult(name(), bucketName(config, request.getBucketName()), request.getPrefix(), items,
                    listing.getNextMarker(), listing.isTruncated(), listing.getCommonPrefixes());
        } catch (ObsException ex) {
            throw toStorageException("Failed to list OBS objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey(config, request.getObjectKey());
        try {
            TemporarySignatureRequest signatureRequest = new TemporarySignatureRequest(HttpMethodEnum.GET,
                    request.getExpireSeconds());
            signatureRequest.setBucketName(bucketName(config, request.getBucketName()));
            signatureRequest.setObjectKey(objectKey);
            TemporarySignatureResponse response = client.createTemporarySignature(signatureRequest);
            return new PresignedUrlResult(name(), bucketName(config, request.getBucketName()), rawObjectKey(request.getObjectKey()),
                    response.getSignedUrl(), Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (ObsException ex) {
            throw toStorageException("Failed to generate OBS presigned url: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public com.peach.response.InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.obs.services.model.InitiateMultipartUploadRequest obsRequest =
                    new com.obs.services.model.InitiateMultipartUploadRequest(actualBucket, actualObjectKey);
            obsRequest.setMetadata(buildMetadata(request));
            if (publicRead || request.isPublicRead()) {
                obsRequest.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            }
            InitiateMultipartUploadResult result = client.initiateMultipartUpload(obsRequest);
            return new com.peach.response.InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), result.getUploadId());
        } catch (Exception ex) {
            throw toStorageException("Failed to initiate OBS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            TemporarySignatureRequest signatureRequest = new TemporarySignatureRequest(HttpMethodEnum.PUT,
                    actualBucket, actualObjectKey, null, request.getExpireSeconds());
            signatureRequest.getQueryParams().put("uploadId", request.getUploadId());
            signatureRequest.getQueryParams().put("partNumber", request.getPartNumber());
            TemporarySignatureResponse response = client.createTemporarySignature(signatureRequest);
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), response.getSignedUrl(),
                    Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (Exception ex) {
            throw toStorageException("Failed to prepare OBS multipart upload part: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public com.peach.response.CompleteMultipartUploadResult completeMultipartUpload(
            com.peach.request.CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.obs.services.model.CompleteMultipartUploadRequest obsRequest =
                    new com.obs.services.model.CompleteMultipartUploadRequest(
                    actualBucket, actualObjectKey, request.getUploadId(), buildPartEtags(request.getParts()));
            CompleteMultipartUploadResult result = client.completeMultipartUpload(obsRequest);
            return new com.peach.response.CompleteMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), request.getUploadId(), result.getEtag(),
                    result.getLocation());
        } catch (Exception ex) {
            throw toStorageException("Failed to complete OBS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.abortMultipartUpload(new com.obs.services.model.AbortMultipartUploadRequest(
                    actualBucket, actualObjectKey, request.getUploadId()));
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to abort OBS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public void setPublicReadAcl(String objectKey) {
        try {
            AccessControlList acl = client.getObjectAcl(config.getBucketName(), objectKey);
            acl.grantPermission(com.obs.services.model.GroupGrantee.ALL_USERS,
                    com.obs.services.model.Permission.PERMISSION_READ);
            client.setObjectAcl(config.getBucketName(), objectKey, acl);
        } catch (ObsException ex) {
            throw toStorageException("Failed to set OBS public read acl: " + objectKey, ex);
        }
    }

    @Override
    public Set<StorageCapability> capabilities() {
        Set<StorageCapability> capabilities = baseCapabilities(true);
        capabilities.add(StorageCapability.PRESIGNED_PUT_URL);
        capabilities.add(StorageCapability.MULTIPART_UPLOAD);
        return capabilities;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    /**
     * 为普通对象上传请求构建元数据。
     *
     * <p>从 {@link UploadObjectRequest} 中提取内容长度、内容类型和自定义元数据，
     * 并转换为 OBS SDK 所需的 {@link ObjectMetadata} 对象。</p>
     *
     * @param request 上传对象请求，包含内容长度、内容类型和自定义元数据
     * @return 配置完成的 OBS 对象元数据
     * @throws Exception 获取内容长度时发生异常
     */
    private ObjectMetadata buildMetadata(UploadObjectRequest request) throws IOException {
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
     * 并转换为 OBS SDK 所需的 {@link ObjectMetadata} 对象。分片上传不需要预设内容长度。</p>
     *
     * @param request 分片上传初始化请求，包含内容类型和自定义元数据
     * @return 配置完成的 OBS 对象元数据
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
     * @return 配置完成的 OBS 对象元数据
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
     * 将分片信息列表转换为按分片号排序的 PartEtag 列表。
     *
     * <p>OBS 完成分片上传接口要求提供按分片号升序排列的 PartEtag 列表，
     * 此方法负责转换和排序。</p>
     *
     * @param parts 分片信息列表，包含每个分片的 ETag 和分片号
     * @return 按分片号升序排列的 PartEtag 列表
     */
    private List<PartEtag> buildPartEtags(List<Part> parts) {
        return parts.stream()
                .map(part -> new PartEtag(part.getETag(), part.getPartNumber()))
                .sorted(Comparator.comparingInt(PartEtag::getPartNumber))
                .toList();
    }


}
