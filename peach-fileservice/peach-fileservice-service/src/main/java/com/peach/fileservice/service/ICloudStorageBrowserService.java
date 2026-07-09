package com.peach.fileservice.service;

import com.peach.fileservice.dto.CloudStorageListDTO;
import com.peach.fileservice.vo.CloudStorageObjectNodeVO;
import com.peach.fileservice.vo.CloudStorageObjectPageVO;
import com.peach.fileservice.vo.CloudStorageUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 云存储浏览服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
public interface ICloudStorageBrowserService {

    /**
     * 检查存储桶是否存在。
     *
     * @param instanceId 存储实例ID
     * @return true表示存在
     */
    boolean bucketExists(String instanceId);

    /**
     * 检查对象是否存在。
     *
     * @param instanceId 存储实例ID
     * @param objectKey 对象Key
     * @return true表示存在
     */
    boolean objectExists(String instanceId, String objectKey);

    /**
     * 查询对象列表。
     *
     * @param instanceId 存储实例ID
     * @param data 查询参数
     * @return 对象列表
     */
    CloudStorageObjectPageVO list(String instanceId, CloudStorageListDTO data);

    /**
     * 查询对象元数据。
     *
     * @param instanceId 存储实例ID
     * @param objectKey 对象Key
     * @return 对象节点信息
     */
    CloudStorageObjectNodeVO stat(String instanceId, String objectKey);

    /**
     * 上传云存储对象。
     *
     * @param instanceId 存储实例ID
     * @param targetPath 目标路径
     * @param file 上传文件
     * @return 上传结果
     */
    CloudStorageUploadVO upload(String instanceId, String targetPath, MultipartFile file);

    /**
     * 创建目录。
     *
     * @param instanceId 存储实例ID
     * @param path 目录路径
     */
    void createDirectory(String instanceId, String path);

    /**
     * 删除对象。
     *
     * @param instanceId 存储实例ID
     * @param objectKey 对象Key
     */
    void deleteObject(String instanceId, String objectKey);

    /**
     * 删除目录。
     *
     * @param instanceId 存储实例ID
     * @param path 目录路径
     */
    void deleteDirectory(String instanceId, String path);
}