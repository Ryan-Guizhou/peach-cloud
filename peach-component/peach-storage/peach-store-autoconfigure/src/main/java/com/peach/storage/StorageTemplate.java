package com.peach.storage;

import com.peach.content.UploadContent;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.FrontendUploadTokenRequest;
import com.peach.request.HeadObjectRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadPartRequest;
import com.peach.request.CompleteMultipartUploadRequest;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.UploadObjectRequest;
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
import com.peach.storage.spi.StorageProvider;
import com.peach.util.StorageLogSanitizer;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 存储操作模板入口。
 *
 * <p>所有不指定 `providerName` 的方法都会委托给 `primary` 主存储实例。如果需要操作其他存储，
 * 使用 {@link #provider(String)} 获取指定实例，或调用带 `providerName` 的重载方法。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 13:49
 */
@Slf4j
public class StorageTemplate {

    private final StorageProvider primaryProvider;

    private final Map<String, StorageProvider> namedProviders;

    public StorageTemplate(StorageProvider primaryProvider, Map<String, StorageProvider> namedProviders) {
        this.primaryProvider = primaryProvider;
        this.namedProviders = Collections.unmodifiableMap(namedProviders);
        log.info("StorageTemplate created. primary={}, providers={}",
                primaryProvider == null ? null : primaryProvider.name(),
                StorageLogSanitizer.providerNames(this.namedProviders));
    }

    /**
     * 获取默认 provider。
     *
     * @return 默认 provider
     */
    public StorageProvider primary() {
        if (primaryProvider == null) {
            log.error("Primary storage provider is not configured. availableProviders={}",
                    StorageLogSanitizer.providerNames(namedProviders));
            throw new IllegalStateException("No primary StorageProvider configured. Available providers: "
                    + namedProviders.keySet());
        }
        return primaryProvider;
    }
    /**
     * 按名称获取 provider。
     *
     * @param providerName provider 名称
     * @return provider 实例
     */
    public StorageProvider provider(String providerName) {
        StorageProvider provider = namedProviders.get(providerName);
        if (provider == null) {
            log.error("Storage provider lookup failed. providerName={}, availableProviders={}",
                    providerName, StorageLogSanitizer.providerNames(namedProviders));
            throw new IllegalArgumentException("Storage provider not found: " + providerName
                    + ". Available providers: " + namedProviders.keySet());
        }
        return provider;
    }


    /**
     * 获取默认 bucket 名称。
     *
     * @return 默认 bucket 名称
     */
    public String bucketName() {
        return primary().bucketName();
    }

    /**
     * 使用默认 provider 上传对象。
     *
     * @param objectKey 对象 key
     * @param inputStream 内容流
     * @param contentLength 内容长度
     */
    public void upload(String objectKey, InputStream inputStream, long contentLength) {
        primary().upload(objectKey, inputStream, contentLength);
    }

    /**
     * 使用默认 provider 上传对象并指定 Content-Type。
     *
     * @param objectKey 对象 key
     * @param inputStream 内容流
     * @param contentLength 内容长度
     * @param contentType MIME 类型
     */
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        primary().upload(objectKey, inputStream, contentLength, contentType);
    }

    /**
     * 使用默认 provider 上传对象。
     *
     * @param objectKey 对象 key
     * @param content 上传内容
     * @param contentType MIME 类型
     * @return 上传结果
     */
    public UploadResult upload(String objectKey, UploadContent content, String contentType) {
        return upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(content)
                .contentType(contentType)
                .build());
    }

    /**
     * 使用默认 provider 上传对象。
     *
     * @param request 上传请求
     * @return 上传结果
     */
    public UploadResult upload(UploadObjectRequest request) {
        return primary().upload(request);
    }

    /**
     * 使用指定 provider 上传对象。
     *
     * @param providerName provider 名称
     * @param request 上传请求
     * @return 上传结果
     */
    public UploadResult upload(String providerName, UploadObjectRequest request) {
        return provider(providerName).upload(request);
    }

    /**
     * 使用默认 provider 下载对象。
     *
     * @param objectKey 对象 key
     * @return 对象内容流，由调用方负责关闭
     */
    public InputStream download(String objectKey) {
        return primary().download(objectKey);
    }

    /**
     * 使用默认 provider 下载对象。
     *
     * @param request 下载请求
     * @return 对象内容流，由调用方负责关闭
     */
    public InputStream download(DownloadObjectRequest request) {
        return primary().download(request);
    }

    /**
     * 使用指定 provider 下载对象。
     *
     * @param providerName provider 名称
     * @param request 下载请求
     * @return 对象内容流，由调用方负责关闭
     */
    public InputStream download(String providerName, DownloadObjectRequest request) {
        return provider(providerName).download(request);
    }

    /**
     * 使用默认 provider 删除对象。
     *
     * @param objectKey 对象 key
     */
    public void delete(String objectKey) {
        primary().delete(objectKey);
    }

    /**
     * 使用默认 provider 删除对象。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    public DeleteResult delete(DeleteObjectRequest request) {
        return primary().delete(request);
    }

    /**
     * 使用指定 provider 删除对象。
     *
     * @param providerName provider 名称
     * @param request 删除请求
     * @return 删除结果
     */
    public DeleteResult delete(String providerName, DeleteObjectRequest request) {
        return provider(providerName).delete(request);
    }

    /**
     * 使用默认 provider 批量删除对象。
     *
     * @param request 批量删除请求
     * @return 批量删除结果
     */
    public BatchDeleteResult batchDelete(BatchDeleteObjectsRequest request) {
        return primary().batchDelete(request);
    }

    /**
     * 使用指定 provider 批量删除对象。
     *
     * @param providerName provider 名称
     * @param request 批量删除请求
     * @return 批量删除结果
     */
    public BatchDeleteResult batchDelete(String providerName, BatchDeleteObjectsRequest request) {
        return provider(providerName).batchDelete(request);
    }

    /**
     * 使用默认 provider 拷贝对象。
     *
     * @param request 拷贝请求
     * @return 拷贝结果
     */
    public CopyResult copy(CopyObjectRequest request) {
        return primary().copy(request);
    }

    /**
     * 使用指定 provider 拷贝对象。
     *
     * @param providerName provider 名称
     * @param request 拷贝请求
     * @return 拷贝结果
     */
    public CopyResult copy(String providerName, CopyObjectRequest request) {
        return provider(providerName).copy(request);
    }

    /**
     * 使用默认 provider 移动对象。
     *
     * @param request 移动请求
     * @return 移动结果
     */
    public MoveResult move(MoveObjectRequest request) {
        return primary().move(request);
    }

    /**
     * 使用指定 provider 移动对象。
     *
     * @param providerName provider 名称
     * @param request 移动请求
     * @return 移动结果
     */
    public MoveResult move(String providerName, MoveObjectRequest request) {
        return provider(providerName).move(request);
    }

    /**
     * 判断默认 provider 中对象是否存在。
     *
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    public boolean exists(String objectKey) {
        return primary().exists(objectKey);
    }

    /**
     * 判断指定 provider 中对象是否存在。
     *
     * @param providerName provider 名称
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    public boolean exists(String providerName, String objectKey) {
        return provider(providerName).exists(objectKey);
    }

    /**
     * 使用默认 provider 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    public ObjectInfo stat(DownloadObjectRequest request) {
        return primary().stat(request);
    }

    /**
     * 使用默认 provider 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    public ObjectInfo head(HeadObjectRequest request) {
        return primary().head(request);
    }

    /**
     * 使用指定 provider 查询对象元信息。
     *
     * @param providerName provider 名称
     * @param request 查询请求
     * @return 对象元信息
     */
    public ObjectInfo stat(String providerName, DownloadObjectRequest request) {
        return provider(providerName).stat(request);
    }

    /**
     * 使用指定 provider 查询对象元信息。
     *
     * @param providerName provider 名称
     * @param request 查询请求
     * @return 对象元信息
     */
    public ObjectInfo head(String providerName, HeadObjectRequest request) {
        return provider(providerName).head(request);
    }

    /**
     * 使用默认 provider 查询对象列表。
     *
     * @param request 列表请求
     * @return 对象列表结果
     */
    public ListObjectsResult list(ListObjectsRequest request) {
        return primary().list(request);
    }

    /**
     * 使用指定 provider 查询对象列表。
     *
     * @param providerName provider 名称
     * @param request 列表请求
     * @return 对象列表结果
     */
    public ListObjectsResult list(String providerName, ListObjectsRequest request) {
        return provider(providerName).list(request);
    }

    /**
     * 使用默认 provider 生成预签名 URL。
     *
     * @param objectKey 对象 key
     * @param expireSeconds 有效期，单位秒
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String objectKey, long expireSeconds) {
        return primary().generatePresignedUrl(objectKey, expireSeconds);
    }

    /**
     * 使用默认 provider 生成预签名 URL。
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        return primary().generatePresignedUrl(request);
    }

    /**
     * 使用指定 provider 生成预签名 URL。
     *
     * @param providerName provider 名称
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    public PresignedUrlResult generatePresignedUrl(String providerName, PresignedUrlRequest request) {
        return provider(providerName).generatePresignedUrl(request);
    }

    /**
     * 使用默认 provider 生成前端直传令牌。
     *
     * @param request 前端直传令牌请求
     * @return 前端直传令牌结果
     */
    public FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request) {
        return primary().createFrontendUploadToken(request);
    }

    /**
     * 使用指定 provider 生成前端直传令牌。
     *
     * @param providerName provider 名称
     * @param request 前端直传令牌请求
     * @return 前端直传令牌结果
     */
    public FrontendUploadTokenResult createFrontendUploadToken(String providerName, FrontendUploadTokenRequest request) {
        return provider(providerName).createFrontendUploadToken(request);
    }

    /**
     * 使用默认 provider 初始化分片上传。
     *
     * @param request 初始化分片上传请求
     * @return 初始化分片上传结果
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        return primary().initiateMultipartUpload(request);
    }

    /**
     * 使用指定 provider 初始化分片上传。
     *
     * @param providerName provider 名称
     * @param request 初始化分片上传请求
     * @return 初始化分片上传结果
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(String providerName, InitiateMultipartUploadRequest request) {
        return provider(providerName).initiateMultipartUpload(request);
    }

    /**
     * 使用默认 provider 生成分片上传信息。
     *
     * @param request 分片上传信息请求
     * @return 分片上传信息结果
     */
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        return primary().prepareUploadPart(request);
    }

    /**
     * 使用指定 provider 生成分片上传信息。
     *
     * @param providerName provider 名称
     * @param request 分片上传信息请求
     * @return 分片上传信息结果
     */
    public UploadPartResult prepareUploadPart(String providerName, UploadPartRequest request) {
        return provider(providerName).prepareUploadPart(request);
    }

    /**
     * 使用默认 provider 完成分片上传。
     *
     * @param request 完成分片上传请求
     * @return 完成分片上传结果
     */
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        return primary().completeMultipartUpload(request);
    }

    /**
     * 使用指定 provider 完成分片上传。
     *
     * @param providerName provider 名称
     * @param request 完成分片上传请求
     * @return 完成分片上传结果
     */
    public CompleteMultipartUploadResult completeMultipartUpload(String providerName,
                                                                 CompleteMultipartUploadRequest request) {
        return provider(providerName).completeMultipartUpload(request);
    }

    /**
     * 使用默认 provider 中止分片上传。
     *
     * @param request 中止分片上传请求
     * @return 中止分片上传结果
     */
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        return primary().abortMultipartUpload(request);
    }

    /**
     * 使用指定 provider 中止分片上传。
     *
     * @param providerName provider 名称
     * @param request 中止分片上传请求
     * @return 中止分片上传结果
     */
    public AbortMultipartUploadResult abortMultipartUpload(String providerName, AbortMultipartUploadRequest request) {
        return provider(providerName).abortMultipartUpload(request);
    }

    /**
     * 从 URL 中提取对象 key，默认按主 provider bucket 规则处理。
     *
     * @param url 完整 URL 或对象 key
     * @return 纯 objectKey
     */
    public String extractObjectKey(String url) {
        return extractObjectKey(url, bucketName());
    }

    /**
     * 从完整 URL 或带签名 query 的对象 key 中提取纯 objectKey。
     *
     * @param url 完整 URL 或对象 key
     * @param bucketName 桶名，path-style URL 会剥离该前缀
     * @return 去除 scheme、host、bucket path-style 前缀和 query 后的 objectKey
     */
    public static String extractObjectKey(String url, String bucketName) {
        if (url == null || url.isBlank()) {
            return url;
        }
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                int queryIndex = url.indexOf('?');
                String key = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
                return removeStart(key, "/");
            }
            URI uri = URI.create(url);
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return "";
            }
            path = removeStart(path, "/");
            if (bucketName != null && !bucketName.isBlank() && path.startsWith(bucketName + "/")) {
                path = path.substring(bucketName.length() + 1);
            }
            return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return url;
        }
    }

    private static String removeStart(String value, String prefix) {
        if (value == null || prefix == null) {
            return value;
        }
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
