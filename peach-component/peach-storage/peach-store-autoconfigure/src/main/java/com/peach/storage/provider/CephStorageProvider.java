package com.peach.storage.provider;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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
import com.peach.request.ListObjectsRequest;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ceph RGW 存储实现。
 *
 * <p>该实现默认通过 S3 兼容协议访问 Ceph RGW，复用 AWS S3 SDK。
 * 配置中必须提供 endpoint、region、accessKey、secretKey 和 bucketName。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 10:20
 */
public class CephStorageProvider implements StorageProvider {

    private final StorageProperties.StorageProvider config;

    private final AmazonS3 client;

    private final String domain;

    private final boolean publicRead;

    public CephStorageProvider(StorageProperties.StorageProvider config) {
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
        return StorageType.CEPH;
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
            throw toStorageException("Failed to check CEPH object exists: " + objectKey, ex);
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
                PutObjectRequest putObjectRequest = new PutObjectRequest(actualBucket, actualObjectKey,
                        inputStream, buildMetadata(request));
                if (publicRead || request.isPublicRead()) {
                    putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
                }
                result = client.putObject(putObjectRequest);
            } finally {
                request.getContent().close();
            }
            return new UploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getContent().length(), publicUrl(domain, actualObjectKey), result.getETag());
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw toStorageException("Failed to upload CEPH object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            S3Object object = client.getObject(new GetObjectRequest(actualBucket, actualObjectKey));
            return object.getObjectContent();
        } catch (Exception ex) {
            throw toStorageException("Failed to download CEPH object: " + request.getObjectKey(), ex);
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
            throw toStorageException("Failed to delete CEPH object: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.doesBucketExistV2(bucketName(config, bucketName));
        } catch (Exception ex) {
            throw toStorageException("Failed to check CEPH bucket exists: " + bucketName, ex);
        }
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            ObjectMetadata metadata = client.getObjectMetadata(new GetObjectMetadataRequest(actualBucket, actualObjectKey));
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
            throw toStorageException("Failed to stat CEPH object: " + request.getObjectKey(), ex);
        }
    }


    @Override
    public ListObjectsResult list(ListObjectsRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String prefix = request.getPrefix() == null || request.getPrefix().isBlank()
                ? null : buildObjectKey(config, request.getPrefix());
        try {
            ListObjectsV2Request listRequest = new ListObjectsV2Request()
                    .withBucketName(actualBucket)
                    .withPrefix(prefix)
                    .withMaxKeys(request.getMaxKeys())
                    .withContinuationToken(request.getContinuationToken());
            String delimiter = resolveDelimiter(request);
            if (delimiter != null) {
                listRequest.withDelimiter(delimiter);
            }
            ListObjectsV2Result listing = client.listObjectsV2(listRequest);
            List<ObjectInfo> items = new ArrayList<>();
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                items.add(ObjectInfo.builder()
                        .providerName(name())
                        .bucketName(summary.getBucketName())
                        .objectKey(businessObjectKey(config, summary.getKey()))
                        .size(summary.getSize())
                        .etag(summary.getETag())
                        .lastModified(toInstant(summary.getLastModified()))
                        .build());
            }
            return buildListResult(name(), actualBucket, request.getPrefix(), items,
                    listing.getNextContinuationToken(), listing.isTruncated(), listing.getCommonPrefixes());
        } catch (NoSuchMethodError error) {
            return fallbackList(config,client);
        } catch (Exception ex) {
            throw toStorageException("Failed to list CEPH objects by prefix: " + request.getPrefix(), ex);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expires = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(actualBucket,
                    actualObjectKey).withMethod(HttpMethod.GET).withExpiration(expires);
            String url = client.generatePresignedUrl(presignedUrlRequest).toString();
            return new PresignedUrlResult(name(), actualBucket, rawObjectKey(request.getObjectKey()), url,
                    expires.toInstant());
        } catch (Exception ex) {
            throw toStorageException("Failed to generate CEPH presigned url: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            com.amazonaws.services.s3.model.InitiateMultipartUploadRequest s3Request =
                    new com.amazonaws.services.s3.model.InitiateMultipartUploadRequest(actualBucket, actualObjectKey);
            s3Request.setObjectMetadata(buildMetadata(request));
            if (publicRead || request.isPublicRead()) {
                s3Request.withCannedACL(CannedAccessControlList.PublicRead);
            }
            com.amazonaws.services.s3.model.InitiateMultipartUploadResult result =
                    client.initiateMultipartUpload(s3Request);
            return new InitiateMultipartUploadResult(name(), actualBucket,
                    rawObjectKey(request.getObjectKey()), result.getUploadId());
        } catch (Exception ex) {
            throw toStorageException("Failed to initiate CEPH multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        Date expires = new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L);
        try {
            GeneratePresignedUrlRequest presignedRequest =
                    new GeneratePresignedUrlRequest(actualBucket, actualObjectKey, HttpMethod.PUT);
            presignedRequest.setExpiration(expires);
            presignedRequest.addRequestParameter("uploadId", request.getUploadId());
            presignedRequest.addRequestParameter("partNumber", String.valueOf(request.getPartNumber()));
            String url = client.generatePresignedUrl(presignedRequest).toString();
            return new UploadPartResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), request.getPartNumber(), url, expires.toInstant());
        } catch (Exception ex) {
            throw toStorageException("Failed to prepare CEPH multipart upload part: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            com.peach.request.CompleteMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            List<PartETag> partETags = buildPartETags(request.getParts());
            com.amazonaws.services.s3.model.CompleteMultipartUploadRequest s3Request =
                    new com.amazonaws.services.s3.model.CompleteMultipartUploadRequest(
                            actualBucket, actualObjectKey, request.getUploadId(), partETags);
            com.amazonaws.services.s3.model.CompleteMultipartUploadResult result =
                    client.completeMultipartUpload(s3Request);
            return new CompleteMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), result.getETag(), result.getLocation());
        } catch (Exception ex) {
            throw toStorageException("Failed to complete CEPH multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        String actualBucket = bucketName(config, request.getBucketName());
        String actualObjectKey = buildObjectKey(config, request.getObjectKey());
        try {
            client.abortMultipartUpload(new com.amazonaws.services.s3.model.AbortMultipartUploadRequest(
                    actualBucket, actualObjectKey, request.getUploadId()));
            return new AbortMultipartUploadResult(name(), actualBucket, rawObjectKey(request.getObjectKey()),
                    request.getUploadId(), true);
        } catch (Exception ex) {
            throw toStorageException("Failed to abort CEPH multipart upload: " + request.getObjectKey(), ex);
        }
    }

    @Override
    public void setPublicReadAcl(String objectKey) {
        try {
            client.setObjectAcl(bucketName(), buildObjectKey(config, objectKey), CannedAccessControlList.PublicRead);
        } catch (Exception ex) {
            throw toStorageException("Failed to set CEPH public read acl: " + objectKey, ex);
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
     * 创建 Ceph S3 兼容客户端。
     *
     * <p>已显式 {@code Protocol.HTTPS}；Sonar S6263 已在根 {@code pom.xml} 多条件忽略。</p>
     *
     * @param config 存储提供者配置
     * @return Ceph S3 客户端
     */
    private AmazonS3 createClient(StorageProperties.StorageProvider config) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setProtocol(Protocol.HTTPS);
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        normalizeEndpoint(config.getEndpoint()), config.getRegion()))
                .withPathStyleAccessEnabled(config.isPathStyleAccess())
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .disableChunkedEncoding()
                .build();
    }

    private ObjectMetadata buildMetadata(UploadObjectRequest request) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        long length = request.getContent().length();
        if (length >= 0) {
            metadata.setContentLength(length);
        }
        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            metadata.setContentType(request.getContentType());
        }
        for (Map.Entry<String, String> entry : request.getMetadata().entrySet()) {
            metadata.addUserMetadata(entry.getKey(), entry.getValue());
        }
        return metadata;
    }

    private ObjectMetadata buildMetadata(InitiateMultipartUploadRequest request) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            metadata.setContentType(request.getContentType());
        }
        for (Map.Entry<String, String> entry : request.getMetadata().entrySet()) {
            metadata.addUserMetadata(entry.getKey(), entry.getValue());
        }
        return metadata;
    }

    private List<PartETag> buildPartETags(List<Part> parts) {
        List<PartETag> partETags = new ArrayList<>();
        for (Part part : parts) {
            partETags.add(new PartETag(part.getPartNumber(), part.getETag()));
        }
        partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));
        return partETags;
    }


}
