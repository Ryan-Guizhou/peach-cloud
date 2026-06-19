package com.peach.storage.provider;

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
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 腾讯云 COS 存储实现。
 *
 * <p>该实现使用腾讯云官方 {@code cos_api}，不经过通用 S3 协议适配。
 * 配置中必须提供 endpoint、region、accessKey、secretKey 和 bucketName。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 10:20
 */
public class CosStorageProvider implements StorageProvider {

    private final StorageProperties.StorageProvider config;
    private final COSClient client;
    private final String domain;
    private final boolean publicRead;

    public CosStorageProvider(StorageProperties.StorageProvider config) {
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
        return StorageType.COS;
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
            throw toStorageException("Failed to check COS object exists: " + objectKey, ex);
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
            PutObjectResult result;
            try (InputStream inputStream = request.getContent().read()) {
                PutObjectRequest putRequest = new PutObjectRequest(actualBucket, actualObjectKey, inputStream,
                        buildMetadata(request));
                result = client.putObject(putRequest);
            } finally {
                request.getContent().close();
            }
            if (publicRead || request.isPublicRead()) {
                client.setObjectAcl(actualBucket, actualObjectKey,
                        com.qcloud.cos.model.CannedAccessControlList.PublicRead);
            }
            return new UploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getContent().length(), publicUrl(domain, actualObjectKey), result.getETag());
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to upload COS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            COSObject object = client.getObject(new GetObjectRequest(actualBucket, actualObjectKey));
            return object.getObjectContent();
        } catch (Exception ex) {
            throw toStorageException("Failed to download COS object: " + request.getObjectKey(), ex);
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
            throw toStorageException("Failed to delete COS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.doesBucketExist(bucketName(config, bucketName));
        } catch (Exception ex) {
            throw toStorageException("Failed to check COS bucket exists: " + bucketName, ex);
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
            throw toStorageException("Failed to stat COS object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public ListObjectsResult list(com.peach.request.ListObjectsRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        try {
            com.qcloud.cos.model.ListObjectsRequest listRequest = new com.qcloud.cos.model.ListObjectsRequest();
            Optional.ofNullable(request.getPrefix())
                            .ifPresent(prefix -> listRequest.setPrefix(prefix));
            Optional.ofNullable(resolveDelimiter(request))
                    .ifPresent(delimiter -> listRequest.setDelimiter(delimiter));
            Optional.ofNullable(request.getContinuationToken())
                    .ifPresent(continuationToken -> listRequest.setMarker(request.getContinuationToken()));
            listRequest.setBucketName(actualBucket);
            listRequest.setMaxKeys(request.getMaxKeys());
            ObjectListing listing = client.listObjects(listRequest);
            List<ObjectInfo> items = new ArrayList<>();
            for (COSObjectSummary summary : listing.getObjectSummaries()) {
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
            throw toStorageException("Failed to list COS objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expires = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            String url = client.generatePresignedUrl(actualBucket, actualObjectKey, expires, HttpMethodName.GET)
                    .toString();
            return new PresignedUrlResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), url,
                    expires.toInstant());
        } catch (Exception ex) {
            throw toStorageException("Failed to generate COS presigned url: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.qcloud.cos.model.InitiateMultipartUploadRequest cosRequest =
                    new com.qcloud.cos.model.InitiateMultipartUploadRequest(
                            actualBucket, actualObjectKey, buildMetadata(request));
            if (publicRead || request.isPublicRead()) {
                cosRequest.setCannedACL(com.qcloud.cos.model.CannedAccessControlList.PublicRead);
            }
            com.qcloud.cos.model.InitiateMultipartUploadResult result = client.initiateMultipartUpload(cosRequest);
            return new InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), result.getUploadId());
        } catch (Exception ex) {
            throw toStorageException("Failed to initiate COS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expires = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            com.qcloud.cos.model.GeneratePresignedUrlRequest urlRequest =
                    new com.qcloud.cos.model.GeneratePresignedUrlRequest(
                            actualBucket, actualObjectKey, HttpMethodName.PUT);
            urlRequest.setExpiration(expires);
            urlRequest.addRequestParameter("uploadId", request.getUploadId());
            urlRequest.addRequestParameter("partNumber", String.valueOf(request.getPartNumber()));
            String url = client.generatePresignedUrl(urlRequest).toString();
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), url, expires.toInstant());
        } catch (Exception ex) {
            throw toStorageException("Failed to prepare COS multipart upload part: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            com.peach.request.CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            List<PartETag> partETags = buildPartETags(request.getParts());
            com.qcloud.cos.model.CompleteMultipartUploadRequest cosRequest =
                    new com.qcloud.cos.model.CompleteMultipartUploadRequest(
                            actualBucket, actualObjectKey, request.getUploadId(), partETags);
            com.qcloud.cos.model.CompleteMultipartUploadResult result = client.completeMultipartUpload(cosRequest);
            return new CompleteMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), result.getETag(), result.getLocation());
        } catch (Exception ex) {
            throw toStorageException("Failed to complete COS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.abortMultipartUpload(new com.qcloud.cos.model.AbortMultipartUploadRequest(
                    actualBucket, actualObjectKey, request.getUploadId()));
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to abort COS multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public void setPublicReadAcl(String objectKey) {
        try {
            client.setObjectAcl(bucketName(), buildObjectKey(config, objectKey),
                    com.qcloud.cos.model.CannedAccessControlList.PublicRead);
        } catch (Exception ex) {
            throw toStorageException("Failed to set COS public read acl: " + objectKey, ex);
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


    /**
     * 创建腾讯云 COS 客户端。
     * @param config
     * @return
     */
    private COSClient createClient(StorageProperties.StorageProvider config) {
        COSCredentials credentials = new BasicCOSCredentials(config.getAccessKey(), config.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
        Optional.ofNullable(resolveEndpointHost(config.getEndpoint()))
                .ifPresent(endpoint -> clientConfig.setEndPointSuffix(endpoint));
        return new COSClient(credentials, clientConfig);
    }

    /**
     * 构建普通上传请求的对象元数据。
     * @param request
     * @return
     * @throws Exception
     */
    private ObjectMetadata buildMetadata(UploadObjectRequest request) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();

        long length = request.getContent().length();
        if (length >= 0) {
            metadata.setContentLength(length);
        }

        fillMetadata(metadata, request.getContentType(), request.getMetadata());
        return metadata;
    }

    /**
     * 构建分片上传初始化请求的对象元数据。
     * @param request
     * @return
     */
    private ObjectMetadata buildMetadata(InitiateMultipartUploadRequest request) {
        ObjectMetadata metadata = new ObjectMetadata();
        fillMetadata(metadata, request.getContentType(), request.getMetadata());
        return metadata;
    }

    /**
     * 填充通用对象元数据。
     * @param metadata
     * @param contentType
     * @param userMetadata
     */
    private void fillMetadata(ObjectMetadata metadata, String contentType, Map<String, String> userMetadata) {
        Optional.ofNullable(contentType)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .ifPresent(metadata::setContentType);

        Optional.ofNullable(userMetadata)
                .orElse(Collections.emptyMap())
                .forEach(metadata::addUserMetadata);
    }

    /**
     * 构建分片上传完成所需的 PartETag 列表，并按分片序号排序。
     * @param parts
     * @return
     */
    private List<PartETag> buildPartETags(List<Part> parts) {
        return Optional.ofNullable(parts)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                .collect(Collectors.toList());
    }



}
