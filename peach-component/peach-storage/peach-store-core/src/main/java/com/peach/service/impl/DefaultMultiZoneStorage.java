package com.peach.service.impl;

import com.peach.config.StorageProperties;
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
import com.peach.service.MultiZoneStorage;
import com.peach.storage.StorageTemplate;
import com.peach.storage.spi.StorageProvider;

import java.io.InputStream;

/**
 * MultiZoneStorage 默认实现。
 *
 * <p>该类只做业务入口委托，不直接引用任何云厂商 SDK 类型。所有云厂商差异均下沉到
 * {@link StorageProvider} 实现中。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class DefaultMultiZoneStorage implements MultiZoneStorage {

    private final StorageTemplate storageTemplate;

    private final StorageProperties storageProperties;

    public DefaultMultiZoneStorage(StorageTemplate storageTemplate, StorageProperties storageProperties) {
        this.storageTemplate = storageTemplate;
        this.storageProperties = storageProperties;
    }

    @Override
    public StorageProvider defaultProvider() {
        return storageTemplate.primary();
    }

    @Override
    public StorageProvider provider(String providerName) {
        return storageTemplate.provider(providerName);
    }

    @Override
    public UploadResult upload(UploadObjectRequest request) {
        return storageTemplate.upload(request);
    }

    @Override
    public UploadResult upload(String providerName, UploadObjectRequest request) {
        return storageTemplate.upload(providerName, request);
    }

    @Override
    public InputStream download(DownloadObjectRequest request) {
        return storageTemplate.download(request);
    }

    @Override
    public InputStream download(String providerName, DownloadObjectRequest request) {
        return storageTemplate.download(providerName, request);
    }

    @Override
    public DeleteResult delete(DeleteObjectRequest request) {
        return storageTemplate.delete(request);
    }

    @Override
    public DeleteResult delete(String providerName, DeleteObjectRequest request) {
        return storageTemplate.delete(providerName, request);
    }

    @Override
    public BatchDeleteResult batchDelete(BatchDeleteObjectsRequest request) {
        return storageTemplate.batchDelete(request);
    }

    @Override
    public BatchDeleteResult batchDelete(String providerName, BatchDeleteObjectsRequest request) {
        return storageTemplate.batchDelete(providerName, request);
    }

    @Override
    public CopyResult copy(CopyObjectRequest request) {
        return storageTemplate.copy(request);
    }

    @Override
    public CopyResult copy(String providerName, CopyObjectRequest request) {
        return storageTemplate.copy(providerName, request);
    }

    @Override
    public MoveResult move(MoveObjectRequest request) {
        return storageTemplate.move(request);
    }

    @Override
    public MoveResult move(String providerName, MoveObjectRequest request) {
        return storageTemplate.move(providerName, request);
    }

    @Override
    public boolean exists(String objectKey) {
        return storageTemplate.exists(objectKey);
    }

    @Override
    public boolean exists(String providerName, String objectKey) {
        return storageTemplate.exists(providerName, objectKey);
    }

    @Override
    public ObjectInfo stat(DownloadObjectRequest request) {
        return storageTemplate.stat(request);
    }

    @Override
    public ObjectInfo head(HeadObjectRequest request) {
        return storageTemplate.head(request);
    }

    @Override
    public ObjectInfo stat(String providerName, DownloadObjectRequest request) {
        return storageTemplate.stat(providerName, request);
    }

    @Override
    public ObjectInfo head(String providerName, HeadObjectRequest request) {
        return storageTemplate.head(providerName, request);
    }

    @Override
    public ListObjectsResult list(ListObjectsRequest request) {
        return storageTemplate.list(request);
    }

    @Override
    public ListObjectsResult list(String providerName, ListObjectsRequest request) {
        return storageTemplate.list(providerName, request);
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(PresignedUrlRequest request) {
        return storageTemplate.generatePresignedUrl(request);
    }

    @Override
    public PresignedUrlResult generatePresignedUrl(String providerName, PresignedUrlRequest request) {
        return storageTemplate.generatePresignedUrl(providerName, request);
    }

    @Override
    public FrontendUploadTokenResult createFrontendUploadToken(FrontendUploadTokenRequest request) {
        return storageTemplate.createFrontendUploadToken(request);
    }

    @Override
    public FrontendUploadTokenResult createFrontendUploadToken(String providerName, FrontendUploadTokenRequest request) {
        return storageTemplate.createFrontendUploadToken(providerName, request);
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        return storageTemplate.initiateMultipartUpload(request);
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(String providerName, InitiateMultipartUploadRequest request) {
        return storageTemplate.initiateMultipartUpload(providerName, request);
    }

    @Override
    public UploadPartResult prepareUploadPart(UploadPartRequest request) {
        return storageTemplate.prepareUploadPart(request);
    }

    @Override
    public UploadPartResult prepareUploadPart(String providerName, UploadPartRequest request) {
        return storageTemplate.prepareUploadPart(providerName, request);
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        return storageTemplate.completeMultipartUpload(request);
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(String providerName, CompleteMultipartUploadRequest request) {
        return storageTemplate.completeMultipartUpload(providerName, request);
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(AbortMultipartUploadRequest request) {
        return storageTemplate.abortMultipartUpload(request);
    }

    @Override
    public AbortMultipartUploadResult abortMultipartUpload(String providerName, AbortMultipartUploadRequest request) {
        return storageTemplate.abortMultipartUpload(providerName, request);
    }

    /**
     * 获取当前多存储服务使用的存储配置。
     *
     * @return 存储配置
     */
    public StorageProperties getStorageProperties() {
        return storageProperties;
    }
}