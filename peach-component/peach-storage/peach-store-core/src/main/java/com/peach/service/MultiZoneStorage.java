package com.peach.service;

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
import com.peach.storage.spi.StorageProvider;

import java.io.InputStream;

/**
 * 多存储实例服务接口。
 *
 * <p>对外提供按 provider 路由的统一存储访问入口。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public interface MultiZoneStorage {

    /**
     * 获取默认 provider。
     *
     * @return 默认 provider
     */
    StorageProvider defaultProvider();

    /**
     * 按名称获取 provider。
     *
     * @param providerName provider 名称
     * @return provider 实例
     */
    StorageProvider provider(String providerName);

    /**
     * 使用默认 provider 上传对象。
     *
     * @param request 上传请求
     * @return 上传结果
     */
    UploadResult upload(UploadObjectRequest request);

    /**
     * 使用指定 provider 上传对象。
     *
     * @param providerName provider 名称
     * @param request 上传请求
     * @return 上传结果
     */
    UploadResult upload(String providerName, UploadObjectRequest request);

    /**
     * 使用默认 provider 下载对象。
     *
     * @param request 下载请求
     * @return 对象内容流，由调用方负责关闭
     */
    InputStream download(DownloadObjectRequest request);

    /**
     * 使用指定 provider 下载对象。
     *
     * @param providerName provider 名称
     * @param request 下载请求
     * @return 对象内容流，由调用方负责关闭
     */
    InputStream download(String providerName, DownloadObjectRequest request);

    /**
     * 使用默认 provider 删除对象。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    DeleteResult delete(DeleteObjectRequest request);

    /**
     * 使用指定 provider 删除对象。
     *
     * @param providerName provider 名称
     * @param request 删除请求
     * @return 删除结果
     */
    DeleteResult delete(String providerName, DeleteObjectRequest request);

    /**
     * 使用默认 provider 批量删除对象。
     *
     * @param request 批量删除请求
     * @return 批量删除结果
     */
    BatchDeleteResult batchDelete(BatchDeleteObjectsRequest request);

    /**
     * 使用指定 provider 批量删除对象。
     *
     * @param providerName provider 名称
     * @param request 批量删除请求
     * @return 批量删除结果
     */
    BatchDeleteResult batchDelete(String providerName, BatchDeleteObjectsRequest request);

    /**
     * 使用默认 provider 拷贝对象。
     *
     * @param request 拷贝请求
     * @return 拷贝结果
     */
    CopyResult copy(CopyObjectRequest request);

    /**
     * 使用指定 provider 拷贝对象。
     *
     * @param providerName provider 名称
     * @param request 拷贝请求
     * @return 拷贝结果
     */
    CopyResult copy(String providerName, CopyObjectRequest request);

    /**
     * 使用默认 provider 移动对象。
     *
     * @param request 移动请求
     * @return 移动结果
     */
    MoveResult move(MoveObjectRequest request);

    /**
     * 使用指定 provider 移动对象。
     *
     * @param providerName provider 名称
     * @param request 移动请求
     * @return 移动结果
     */
    MoveResult move(String providerName, MoveObjectRequest request);

    /**
     * 判断默认 provider 中对象是否存在。
     *
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    boolean exists(String objectKey);

    /**
     * 判断指定 provider 中对象是否存在。
     *
     * @param providerName provider 名称
     * @param objectKey 对象 key
     * @return true 表示存在
     */
    boolean exists(String providerName, String objectKey);

    /**
     * 使用默认 provider 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    ObjectInfo stat(DownloadObjectRequest request);

    /**
     * 使用默认 provider 查询对象元信息。
     *
     * @param request 查询请求
     * @return 对象元信息
     */
    ObjectInfo head(HeadObjectRequest request);

    /**
     * 使用指定 provider 查询对象元信息。
     *
     * @param providerName provider 名称
     * @param request 查询请求
     * @return 对象元信息
     */
    ObjectInfo stat(String providerName, DownloadObjectRequest request);

    /**
     * 使用指定 provider 查询对象元信息。
     *
     * @param providerName provider 名称
     * @param request 查询请求
     * @return 对象元信息
     */
    ObjectInfo head(String providerName, HeadObjectRequest request);

    /**
     * 使用默认 provider 查询对象列表。
     *
     * @param request 列表请求
     * @return 对象列表结果
     */
    ListObjectsResult list(ListObjectsRequest request);

    /**
     * 使用指定 provider 查询对象列表。
     *
     * @param providerName provider 名称
     * @param request 列表请求
     * @return 对象列表结果
     */
    ListObjectsResult list(String providerName, ListObjectsRequest request);

    /**
     * 使用默认 provider 生成预签名 URL。
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request);

    /**
     * 使用指定 provider 生成预签名 URL。
     *
     * @param providerName provider 名称
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    PresignedUrlResult generatePresignedUrl(String providerName, PresignedUrlRequest request);

    /**
     * 使用默认 provider 生成前端直传令牌。
     *
     * @param request 前端直传令牌请求
     * @return 前端直传令牌结果
     */
    FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request);

    /**
     * 使用指定 provider 生成前端直传令牌。
     *
     * @param providerName provider 名称
     * @param request 前端直传令牌请求
     * @return 前端直传令牌结果
     */
    FrontendUploadTokenResult createFrontendUploadToken(String providerName, FrontendUploadTokenRequest request);

    /**
     * 使用默认 provider 初始化分片上传。
     *
     * @param request 初始化分片上传请求
     * @return 初始化分片上传结果
     */
    InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request);

    /**
     * 使用指定 provider 初始化分片上传。
     *
     * @param providerName provider 名称
     * @param request 初始化分片上传请求
     * @return 初始化分片上传结果
     */
    InitiateMultipartUploadResult initiateMultipartUpload(String providerName, InitiateMultipartUploadRequest request);

    /**
     * 使用默认 provider 生成分片上传信息。
     *
     * @param request 分片上传信息请求
     * @return 分片上传信息结果
     */
    UploadPartResult prepareUploadPart(UploadPartRequest request);

    /**
     * 使用指定 provider 生成分片上传信息。
     *
     * @param providerName provider 名称
     * @param request 分片上传信息请求
     * @return 分片上传信息结果
     */
    UploadPartResult prepareUploadPart(String providerName, UploadPartRequest request);

    /**
     * 使用默认 provider 完成分片上传。
     *
     * @param request 完成分片上传请求
     * @return 完成分片上传结果
     */
    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request);

    /**
     * 使用指定 provider 完成分片上传。
     *
     * @param providerName provider 名称
     * @param request 完成分片上传请求
     * @return 完成分片上传结果
     */
    CompleteMultipartUploadResult completeMultipartUpload(String providerName, CompleteMultipartUploadRequest request);

    /**
     * 使用默认 provider 中止分片上传。
     *
     * @param request 中止分片上传请求
     * @return 中止分片上传结果
     */
    AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request);

    /**
     * 使用指定 provider 中止分片上传。
     *
     * @param providerName provider 名称
     * @param request 中止分片上传请求
     * @return 中止分片上传结果
     */
    AbortMultipartUploadResult abortMultipartUpload(String providerName, AbortMultipartUploadRequest request);
}