package com.peach.storage.provider;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.http.HttpMethodName;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.BosObject;
import com.baidubce.services.bos.model.CompleteMultipartUploadResponse;
import com.baidubce.services.bos.model.GeneratePresignedUrlRequest;
import com.baidubce.services.bos.model.InitiateMultipartUploadResponse;
import com.baidubce.services.bos.model.ListObjectsRequest;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.baidubce.services.bos.model.ObjectMetadata;
import com.baidubce.services.bos.model.PartETag;
import com.baidubce.services.bos.model.PutObjectRequest;
import com.baidubce.services.bos.model.PutObjectResponse;
import com.baidubce.services.bos.model.SetObjectAclRequest;
import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageResultCode;
import com.peach.enums.StorageType;
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
import com.peach.response.CompleteMultipartUploadResult;
import com.peach.response.DeleteResult;
import com.peach.response.InitiateMultipartUploadResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.storage.spi.StorageProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
/**
 * 百度云 BOS 存储实现。
 *
 * <p>该实现使用百度云官方 {@code bce-java-sdk}，不经过通用 S3 协议适配。
 * 配置中必须提供 endpoint、accessKey、secretKey 和 bucketName。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 10:20
 */
public class BosStorageProvider implements StorageProvider {

    private final StorageProperties.StorageProvider config;
    private final BosClient client;
    private final String domain;
    private final boolean publicRead;

    public BosStorageProvider(StorageProperties.StorageProvider config) {
        this.config = config;
        this.client = createClient(config);
        this.domain = config.getDomain();
        this.publicRead = config.isPublicRead();
    }

    @Override
    public String bucketName() {
        return bucketName(config);
    }

    @Override
    public StorageType storageType() {
        return StorageType.BOS;
    }

    @Override
    public String name() {
        return name(config);
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        try {
            return client.doesObjectExist(bucketName(config, bucketName), buildObjectKey(config, objectKey));
        } catch (Exception ex) {
            throw toStorageException("Failed to check BOS object exists: " + objectKey, ex);
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
            PutObjectResponse result;
            try (InputStream inputStream = request.getContent().read()) {
                PutObjectRequest putRequest = new PutObjectRequest(actualBucket, actualObjectKey, inputStream);
                putRequest.setObjectMetadata(buildMetadata(request));
                result = client.putObject(putRequest);
            } finally {
                request.getContent().close();
            }
            if (publicRead || request.isPublicRead()) {
                client.setObjectAcl(new SetObjectAclRequest(actualBucket, actualObjectKey,
                        com.baidubce.services.bos.model.CannedAccessControlList.PublicRead));
            }
            return new UploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getContent().length(), publicUrl(domain, actualObjectKey), result.getETag());
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to upload BOS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            BosObject object = client.getObject(actualBucket, actualObjectKey);
            return object.getObjectContent();
        } catch (Exception ex) {
            throw toStorageException("Failed to download BOS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.deleteObject(actualBucket, actualObjectKey);
            return new DeleteResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to delete BOS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.doesBucketExist(bucketName(config, bucketName));
        } catch (Exception ex) {
            throw toStorageException("Failed to check BOS bucket exists: " + bucketName, ex);
        }
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            ObjectMetadata metadata = client.getObjectMetadata(actualBucket, actualObjectKey);
            return ObjectInfo.builder()
                    .providerName(name())
                    .bucketName(actualBucket)
                    .objectKey(rawObjectKey(request.getObjectKey()))
                    .size(metadata.getContentLength())
                    .contentType(metadata.getContentType())
                    .etag(metadata.getETag())
                    .lastModified(toInstant(metadata.getLastModified()))
                    .metadata(metadata.getUserMetadata())
                    .build();
        } catch (Exception ex) {
            throw toStorageException("Failed to stat BOS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public ListObjectsResult list(com.peach.request.ListObjectsRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());

        try {
            ListObjectsRequest listRequest = new ListObjectsRequest(actualBucket);
            listRequest.setMaxKeys(request.getMaxKeys());
            Optional.ofNullable(request.getPrefix()).ifPresent(listRequest::setPrefix);
            Optional.ofNullable(request.getContinuationToken()).ifPresent(listRequest::setMarker);
            Optional.ofNullable(resolveDelimiter(request)).ifPresent(listRequest::setDelimiter);

            ListObjectsResponse listing = client.listObjects(listRequest);
            List<ObjectInfo> items = new ArrayList<>();
            for (com.baidubce.services.bos.model.BosObjectSummary summary : listing.getContents()) {
                items.add(ObjectInfo.builder()
                        .providerName(name())
                        .bucketName(actualBucket)
                        .objectKey(businessObjectKey(config, summary.getKey()))
                        .size(summary.getSize())
                        .etag(summary.getETag())
                        .lastModified(toInstant(summary.getLastModified()))
                        .build());
            }
            return buildListResult(name(), actualBucket, request.getPrefix(), items,
                    listing.getNextMarker(), listing.isTruncated(), listing.getCommonPrefixes());
        } catch (Exception ex) {
            throw toStorageException("Failed to list BOS objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Instant expiresAt = Instant.now().plusSeconds(request.getExpireSeconds());
        try {
            GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(actualBucket, actualObjectKey,
                    HttpMethodName.GET);
            urlRequest.setExpiration((int) request.getExpireSeconds());
            String url = client.generatePresignedUrl(urlRequest).toString();
            return new PresignedUrlResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), url,
                    expiresAt);
        } catch (Exception ex) {
            throw toStorageException("Failed to generate BOS presigned url: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.baidubce.services.bos.model.InitiateMultipartUploadRequest bosRequest =
                    new com.baidubce.services.bos.model.InitiateMultipartUploadRequest(actualBucket, actualObjectKey);
            bosRequest.setObjectMetadata(buildMetadata(request));
            InitiateMultipartUploadResponse result = client.initiateMultipartUpload(bosRequest);
            return new InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), result.getUploadId());
        } catch (Exception ex) {
            throw toStorageException("Failed to initiate BOS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            GeneratePresignedUrlRequest urlRequest =
                    new GeneratePresignedUrlRequest(actualBucket, actualObjectKey, HttpMethodName.PUT);
            urlRequest.setExpiration((int) request.getExpireSeconds());
            urlRequest.addRequestParameter("uploadId", request.getUploadId());
            urlRequest.addRequestParameter("partNumber", String.valueOf(request.getPartNumber()));
            String url = client.generatePresignedUrl(urlRequest).toString();
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), url,
                    Instant.now().plusSeconds(request.getExpireSeconds()));
        } catch (Exception ex) {
            throw toStorageException("Failed to prepare BOS multipart upload part: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            com.peach.request.CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.baidubce.services.bos.model.CompleteMultipartUploadRequest bosRequest =
                    new com.baidubce.services.bos.model.CompleteMultipartUploadRequest(
                            actualBucket, actualObjectKey, request.getUploadId(), buildPartETags(request.getParts()));
            CompleteMultipartUploadResponse result = client.completeMultipartUpload(bosRequest);
            return new CompleteMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), result.getETag(), result.getLocation());
        } catch (Exception ex) {
            throw toStorageException("Failed to complete BOS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.abortMultipartUpload(new com.baidubce.services.bos.model.AbortMultipartUploadRequest(
                    actualBucket, actualObjectKey, request.getUploadId()));
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to abort BOS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public void setPublicReadAcl(String objectKey) {
        try {
            client.setObjectAcl(new SetObjectAclRequest(bucketName(), buildObjectKey(config, objectKey),
                    com.baidubce.services.bos.model.CannedAccessControlList.PublicRead));
        } catch (Exception ex) {
            throw toStorageException("Failed to set BOS public read acl: " + objectKey, ex);
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
    public void close() {
        client.shutdown();
    }

    private BosClient createClient(StorageProperties.StorageProvider config) {
        BosClientConfiguration clientConfiguration = new BosClientConfiguration();
        clientConfiguration.setCredentials(new DefaultBceCredentials(config.getAccessKey(), config.getSecretKey()));
        clientConfiguration.setEndpoint(config.getEndpoint());
        clientConfiguration.setPathStyleAccessEnable(config.isPathStyleAccess());
        return new BosClient(clientConfiguration);
    }

    private ObjectMetadata buildMetadata(UploadObjectRequest request) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        long length = request.getContent().length();
        if (length >= 0) {
            metadata.setContentLength(length);
        }
        if (StringUtils.isBlank(request.getContentType())) {
            metadata.setContentType(request.getContentType());
        }
        for (Map.Entry<String, String> entry : request.getMetadata().entrySet()) {
            metadata.addUserMetadata(entry.getKey(), entry.getValue());
        }
        return metadata;
    }

    private ObjectMetadata buildMetadata(InitiateMultipartUploadRequest request) {
        ObjectMetadata metadata = new ObjectMetadata();
        Optional.ofNullable(request.getContentType())
                .ifPresent(metadata::setContentType);
        for (Map.Entry<String, String> entry : request.getMetadata().entrySet()) {
            metadata.addUserMetadata(entry.getKey(), entry.getValue());
        }
        return metadata;
    }

    private List<PartETag> buildPartETags(List<Part> parts) {
        return Optional.ofNullable(parts)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                .toList();
    }


}
