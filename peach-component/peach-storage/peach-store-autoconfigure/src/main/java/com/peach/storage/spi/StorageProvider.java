package com.peach.storage.spi;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageResultCode;
import com.peach.enums.StorageType;
import com.peach.exception.StorageException;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CompleteMultipartUploadRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.FrontendUploadTokenRequest;
import com.peach.request.HeadObjectRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.request.UploadPartRequest;
import com.peach.response.AbortMultipartUploadResult;
import com.peach.response.BatchDeleteResult;
import com.peach.response.CompleteMultipartUploadResult;
import com.peach.response.CopyResult;
import com.peach.response.DeleteResult;
import com.peach.response.FrontendUploadTokenResult;
import com.peach.response.InitiateMultipartUploadResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.MoveResult;
import com.peach.response.ObjectInfo;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.storage.spi.support.StorageProviderSupport;
import com.peach.util.StoragePathUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
/**
 * 存储 provider 运行期 SPI。
 *
 * <p>该接口定义统一的对象存储访问能力，并提供部分通用默认实现。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public interface StorageProvider extends AutoCloseable {

    /**
     * 获取默认 bucket 名称。
     *
     * @return 默认 bucket 名称
     */
    String bucketName();

    /**
     * 获取存储类型。
     *
     * @return 存储类型
     */
    StorageType storageType();

    /**
     * 获取 provider 实例名称。
     *
     * @return provider 实例名称
     */
    String name();


    /**
     * 上传对象。
     *
     * @param request 上传请求
     * @return 上传结果
     */
    UploadResult upload(UploadObjectRequest request);

    /**
     * 下载对象。
     *
     * @param request 下载请求
     * @return 对象内容流，由调用方负责关闭
     */
    InputStream download(DownloadObjectRequest request);

    /**
     * 删除对象。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    DeleteResult delete(DeleteObjectRequest request);

    /**
     * 判断指定 bucket 中对象是否存在。
     *
     * @param bucketName bucket 名称
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    boolean exists(String bucketName, String objectKey);


    /**
     * 判断当前 provider 是否属于无物理 bucket 的存储类型。
     *
     * @return true 表示 bucket 仅作为逻辑别名存在
     */
    default boolean isBucketless() {
        return false;
    }


    /**
     * 判断默认 bucket 中对象是否存在。
     *
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    default boolean exists(String objectKey) {
        return exists(bucketName(), objectKey);
    }


    /**
     * 判断 bucket 或根目录是否存在。
     *
     * @param bucketName bucket 名称
     * @return true 表示存在
     */
    default boolean bucketExists(String bucketName) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "bucketExists");
    }


    /**
     * 批量删除对象。
     *
     * @param request 批量删除请求
     * @return 批量删除结果
     */
    default BatchDeleteResult batchDelete(BatchDeleteObjectsRequest request) {
        if (request == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Batch delete request must not be null");
        }
        int deletedCount = 0;
        for (String objectKey : request.getObjectKeys()) {
            DeleteResult result = delete(DeleteObjectRequest.builder()
                    .bucketName(request.getBucketName())
                    .objectKey(objectKey)
                    .build());
            if (result.isDeleted()) {
                deletedCount++;
            }
        }
        return new BatchDeleteResult(name(), request.getBucketName(), request.getObjectKeys(), deletedCount);
    }

    /**
     * 拷贝对象。
     *
     * @param request 拷贝请求
     * @return 拷贝结果
     */
    default CopyResult copy(CopyObjectRequest request) {
        return StorageProviderSupport.copy(this, request);
    }

    /**
     * 移动对象。
     *
     * @param request 移动请求
     * @return 移动结果
     */
    default MoveResult move(MoveObjectRequest request) {
        return StorageProviderSupport.move(this, request);
    }

    /**
     * 生成前端直传令牌。
     *
     * @param request 前端直传令牌请求
     * @return 前端直传令牌结果
     */
    default FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "frontend upload token");
    }

    /**
     * 初始化分片上传。
     *
     * @param request 初始化分片上传请求
     * @return 初始化分片上传结果
     */
    default InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "multipart upload");
    }

    /**
     * 生成分片上传信息。
     *
     * @param request 分片上传信息请求
     * @return 分片上传信息结果
     */
    default UploadPartResult prepareUploadPart(UploadPartRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "multipart upload");
    }

    /**
     * 完成分片上传。
     *
     * @param request 完成分片上传请求
     * @return 完成分片上传结果
     */
    default CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "multipart upload");
    }

    /**
     * 中止分片上传。
     *
     * @param request 中止分片上传请求
     * @return 中止分片上传结果
     */
    default AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "multipart upload");
    }

    /**
     * 生成预签名 GET URL。
     *
     * @param objectKey 对象 key
     * @param expireSeconds 有效期，单位秒
     * @return 预签名 URL
     */
    default String generatePresignedUrl(String objectKey, long expireSeconds) {
        return generatePresignedUrl(PresignedUrlRequest.builder()
                .objectKey(objectKey)
                .expireSeconds(expireSeconds)
                .build()).getUrl();
    }

    /**
     * 设置对象公共读 ACL。
     *
     * @param objectKey 对象 key
     */
    default void setPublicReadAcl(String objectKey) {
        throw new UnsupportedOperationException(storageType() + " does not support "
                + "setPublicReadAcl");
    }

    /**
     * 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    default ObjectInfo stat(DownloadObjectRequest request) {
        throw new StorageException(StorageResultCode.UNSUPPORTED_OPERATION,
                storageType() + " does not support " + "stat");
    }

    /**
     * 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    default ObjectInfo head(HeadObjectRequest request) {
        return stat(DownloadObjectRequest.builder()
                .bucketName(request.getBucketName())
                .objectKey(request.getObjectKey())
                .build());
    }

    /**
     * 查询对象列表。
     *
     * @param request 列表请求
     * @return 对象列表结果
     */
    default ListObjectsResult list(ListObjectsRequest request) {
        return ListObjectsResult.builder()
                .providerName(name())
                .bucketName(bucketName())
                .prefix(request == null ? null : request.getPrefix())
                .items(List.of())
                .truncated(false)
                .build();
    }

    /**
     * 生成预签名 GET URL 结果对象。
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    default PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        String url = generatePresignedUrl(request.getObjectKey(), request.getExpireSeconds());
        return new PresignedUrlResult(name(), request.getBucketName(), request.getObjectKey(), url,
                Instant.now().plusSeconds(request.getExpireSeconds()));
    }

    /**
     * 返回当前 provider 支持的增强能力集合。
     *
     * @return 增强能力集合
     */
    default Set<StorageCapability> capabilities() {
        return Set.of();
    }

    /**
     * 关闭 provider 持有的底层资源。
     *
     * @throws Exception 关闭资源失败时抛出
     */
    @Override
    default void close() throws Exception {
    }

    /**
     * 根据配置推导 provider 名称。
     *
     * @param config provider 配置
     * @return provider 名称
     */
    default String name(StorageProperties.StorageProvider config) {
        if (config == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Storage provider config must not be null");
        }
        return firstText(config.getName(), config.getType() == null ? null : config.getType().name().toLowerCase());
    }

    /**
     * 解析 bucketless provider 的逻辑别名。
     *
     * @param config provider 配置
     * @return 逻辑存储别名
     */
    default String storageAlias(StorageProperties.StorageProvider config) {
        return firstText(config == null ? null : config.getBucketName(), name(config));
    }

    /**
     * 根据配置推导默认 bucket 名称。
     *
     * @param config provider 配置
     * @return bucket 名称
     */
    default String bucketName(StorageProperties.StorageProvider config) {
        return isBucketless() ? storageAlias(config) : firstText(config.getBucketName(), name(config));
    }

    /**
     * 获取请求实际使用的 bucket 名称。
     *
     * @param config provider 配置
     * @param requestBucketName 请求中的 bucket 名称
     * @return 实际 bucket 名称
     */
    default String bucketName(StorageProperties.StorageProvider config, String requestBucketName) {
        String resolved = bucketName(config);
        if (!isBucketless()) {
            return firstText(requestBucketName, resolved);
        }
        if (requestBucketName == null || requestBucketName.isBlank()) {
            return resolved;
        }
        String requestAlias = requestBucketName.trim();
        if (!resolved.equals(requestAlias)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Bucketless provider does not support overriding bucket alias: " + requestAlias);
        }
        return resolved;
    }

    /**
     * 基于 provider 前缀构造实际对象 key。
     *
     * @param config provider 配置
     * @param objectKey 业务对象 key
     * @return 实际写入或读取的对象 key
     */
    default String buildObjectKey(StorageProperties.StorageProvider config, String objectKey) {
        return StoragePathUtil.applyPrefix(config == null ? null : config.getPrefix(), objectKey);
    }

    /**
     * 规范化业务对象 key。
     *
     * @param objectKey 原始对象 key
     * @return 规范化后的对象 key
     */
    default String rawObjectKey(String objectKey) {
        return StoragePathUtil.normalizeObjectKey(objectKey);
    }

    /**
     * 将带统一前缀的实际对象 key 还原为业务视角对象 key。
     *
     * @param config provider 配置
     * @param actualObjectKey 底层实际对象 key
     * @return 业务视角对象 key
     */
    default String businessObjectKey(StorageProperties.StorageProvider config, String actualObjectKey) {
        return StorageProviderSupport.businessObjectKey(config, actualObjectKey);
    }

    /**
     * 基于自定义域名生成公开访问 URL。
     *
     * @param domain 自定义访问域名
     * @param objectKey 实际对象 key
     * @return 公开访问 URL；未配置域名时返回 null
     */
    default String publicUrl(String domain, String objectKey) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        String normalizedDomain = domain.trim();
        while (normalizedDomain.endsWith("/")) {
            normalizedDomain = normalizedDomain.substring(0, normalizedDomain.length() - 1);
        }
        return normalizedDomain + "/" + StoragePathUtil.normalizeObjectKey(objectKey);
    }

    /**
     * 返回第一个非空白文本。
     *
     * @param first 优先文本
     * @param second 兜底文本
     * @return 第一个非空白文本
     */
    default String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second;
    }

    /**
     * 删除默认 bucket 中对象。
     *
     * @param objectKey 对象 key
     */
    default void delete(String objectKey) {
        delete(DeleteObjectRequest.builder().objectKey(objectKey).build());
    }

    /**
     * 使用输入流上传对象。
     *
     * @param objectKey 对象 key
     * @param inputStream 对象内容流
     * @param contentLength 内容长度
     */
    default void upload(String objectKey, InputStream inputStream, long contentLength) {
        upload(objectKey, inputStream, contentLength, null);
    }

    /**
     * 下载默认 bucket 中对象。
     *
     * @param objectKey 对象 key
     * @return 对象内容流，由调用方负责关闭
     */
    default InputStream download(String objectKey) {
        return download(DownloadObjectRequest.builder().objectKey(objectKey).build());
    }

    /**
     * 判断是否支持公共读 ACL。
     *
     * @return true 表示支持
     */
    default boolean supportsPublicRead() {
        return supports(StorageCapability.PUBLIC_READ_ACL);
    }

    /**
     * 使用输入流上传对象并指定内容类型。
     *
     * @param objectKey 对象 key
     * @param inputStream 对象内容流
     * @param contentLength 内容长度
     * @param contentType 内容类型
     */
    default void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(UploadContent.of(inputStream, contentLength))
                .contentType(contentType)
                .build());
    }


    /**
     * 将 {@link Date} 转为 {@link Instant}。
     *
     * @param date SDK 返回时间
     * @return Instant；为空时返回 null
     */
    default Instant toInstant(Date date) {
        return StorageProviderSupport.toInstant(date);
    }

    /**
     * 规范化 endpoint。
     *
     * @param endpoint 原始 endpoint
     * @return 规范化后的 endpoint
     */
    default String normalizeEndpoint(String endpoint) {
        return StorageProviderSupport.normalizeEndpoint(endpoint);
    }

    /**
     * 解析 endpoint 对应的 host。
     *
     * @param endpoint 原始 endpoint
     * @return endpoint 对应的 host
     */
    default String resolveEndpointHost(String endpoint) {
        return StorageProviderSupport.resolveEndpointHost(endpoint);
    }

    /**
     * 构造统一的对象列表结果。
     *
     * @param providerName provider 名称
     * @param bucketName bucket 名称
     * @param prefix 列表前缀
     * @param items 对象列表
     * @param nextContinuationToken 下一页游标
     * @param truncated 是否还有下一页
     * @param commonPrefixes 目录分组前缀
     * @return 对象列表结果
     */
    default ListObjectsResult buildListResult(String providerName, String bucketName, String prefix,
                                              List<ObjectInfo> items, String nextContinuationToken,
                                              boolean truncated, List<String> commonPrefixes) {
        return StorageProviderSupport.buildListResult(providerName, bucketName, prefix, items,
                nextContinuationToken, truncated, commonPrefixes);
    }

    /**
     * 构造默认能力集合。
     *
     * @param supportsPublicRead 是否支持公共读 ACL
     * @return 默认能力集合
     */
    default Set<StorageCapability> baseCapabilities(boolean supportsPublicRead) {
        return StorageProviderSupport.baseCapabilities(supportsPublicRead);
    }

    /**
     * 返回空的公共目录前缀列表。
     *
     * @return 空列表
     */
    default List<String> noCommonPrefixes() {
        return StorageProviderSupport.noCommonPrefixes();
    }


    /**
     * 判断是否支持指定增强能力。
     *
     * @param capability 增强能力
     * @return true 表示支持
     */
    default boolean supports(StorageCapability capability) {
        return capability != null && capabilities().contains(capability);
    }


    default String resolveDelimiter(ListObjectsRequest request) {
        if (StringUtils.isNotBlank(request.getDelimiter())) {
            return request.getDelimiter();
        }
        return request.isRecursive() ? null : "/";
    }

    default StorageException toStorageException(String message, Exception ex) {
        String text = ex.getMessage();
        if (text != null) {
            if (text.contains("NoSuchKey")) {
                return new StorageException(StorageResultCode.OBJECT_NOT_FOUND, message, ex);
            }
            if (text.contains("NoSuchBucket")) {
                return new StorageException(StorageResultCode.BUCKET_NOT_FOUND, message, ex);
            }
            if (text.contains("AccessDenied") || text.contains("403")) {
                return new StorageException(StorageResultCode.ACCESS_DENIED, message, ex);
            }
        }
        return new StorageException(StorageResultCode.STORAGE_ERROR, message, ex);
    }

    default ListObjectsResult fallbackList(StorageProperties.StorageProvider config, AmazonS3 client) {
        try {
            ObjectListing listing = client.listObjects(config.getBucketName(), config.getPrefix());
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
            return buildListResult(name(), config.getBucketName(), null, items,
                    listing.getNextMarker(), listing.isTruncated(), listing.getCommonPrefixes());
        } catch (Exception ex) {
            throw toStorageException("Failed to list CEPH objects by prefix: " + config.getPrefix(), ex);
        }
    }
}
