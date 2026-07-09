package com.peach.manager;

import com.peach.config.StorageProperties;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.DeleteResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.UploadResult;

/**
 * 基于运行时存储配置的云存储管理操作接口。
 *
 * <p>该服务主要用于存储管理场景，例如连接测试、存储浏览、
 * 文件上传以及目录维护等操作。</p>
 *
 * <p>调用方需要传入临时的 {@link StorageProperties.StorageProvider}
 * 存储配置，服务会根据当前操作动态创建对应的存储实现实例。</p>
 *
 * <p>该接口主要面向管理端场景，不建议用于高频业务文件读写场景。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
public interface CloudStorageManagerService {

    /**
     * 测试存储服务连接是否正常。
     *
     * <p>用于验证当前存储配置是否可以正常访问目标存储空间，
     * 包括 Bucket、根路径等资源是否可达。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @return 如果目标存储空间可以正常访问返回 {@code true}，
     *         否则返回 {@code false}
     */
    boolean testConnection(StorageProperties.StorageProvider providerConfig);

    /**
     * 判断配置的存储空间是否存在。
     *
     * <p>对于对象存储通常对应 Bucket 是否存在，
     * 对于文件系统存储通常对应根目录是否存在。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @return 如果存储空间存在返回 {@code true}，
     *         否则返回 {@code false}
     */
    boolean bucketExists(StorageProperties.StorageProvider providerConfig);

    /**
     * 判断指定对象是否存在。
     *
     * <p>对象路径基于当前存储配置中的逻辑前缀进行解析。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param objectKey 对象相对于配置前缀的对象路径
     * @return 如果对象存在返回 {@code true}，
     *         否则返回 {@code false}
     */
    boolean objectExists(StorageProperties.StorageProvider providerConfig, String objectKey);

    /**
     * 查询指定路径下的对象或目录列表。
     *
     * <p>支持对象存储中的对象列表查询以及文件系统中的目录浏览。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param request 相对于配置前缀的对象列表查询请求
     * @return 统一格式的对象列表查询结果
     */
    ListObjectsResult list(StorageProperties.StorageProvider providerConfig, ListObjectsRequest request);

    /**
     * 查询对象元数据信息。
     *
     * <p>用于获取对象大小、修改时间、类型等基础信息。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param request 相对于配置前缀的对象信息查询请求
     * @return 对象元数据信息
     */
    ObjectInfo stat(StorageProperties.StorageProvider providerConfig, DownloadObjectRequest request);

    /**
     * 上传对象。
     *
     * <p>支持向当前配置的存储目标上传文件或对象。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param request 上传对象请求
     * @return 上传结果信息
     */
    UploadResult upload(StorageProperties.StorageProvider providerConfig, UploadObjectRequest request);

    /**
     * 创建逻辑目录。
     *
     * <p>对于对象存储，由于本身不存在真实目录概念，
     * 通常通过创建目录占位对象实现。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param path 相对于配置前缀的目录路径
     */
    void createDirectory(StorageProperties.StorageProvider providerConfig, String path);

    /**
     * 删除单个对象。
     *
     * @param providerConfig 运行时存储提供方配置
     * @param request 删除对象请求
     * @return 删除结果信息
     */
    DeleteResult deleteObject(StorageProperties.StorageProvider providerConfig, DeleteObjectRequest request);

    /**
     * 递归删除目录。
     *
     * <p>对于对象存储会删除指定前缀下的所有对象，
     * 对于文件系统会递归删除目录及其子文件。</p>
     *
     * @param providerConfig 运行时存储提供方配置
     * @param path 相对于配置前缀的目录路径
     */
    void deleteDirectory(StorageProperties.StorageProvider providerConfig, String path);

}