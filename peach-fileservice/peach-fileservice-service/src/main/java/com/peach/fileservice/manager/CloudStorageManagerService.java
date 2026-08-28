package com.peach.fileservice.manager;

import com.peach.CloudStorageProperties;
import com.peach.fileservice.BucketInfo;

import java.util.List;

/**
 * 云存储管理服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19 16:55
 * @Description 云存储管理服务接口
 */
public interface CloudStorageManagerService extends AutoCloseable{

    /**
     * 通过认证JSON来测试连接
     *
     * @param authJson 认证JSON字符串
     * @return 连接是否成功
     */
    boolean testConnection(String authJson);


    /**
     * 列出指定路径的子目录
     *
     * @param authJson 认证JSON字符串
     * @param path 路径
     * @param storeType 存储类型
     * @return 子目录列表
     */
    List<String> listDir(String authJson,String path,String storeType);


    /**
     * 解析配置文件
     *
     * @param authJson 认证JSON字符串
     * @param storeType 存储类型
     * @return 云存储配置属性
     */
    CloudStorageProperties parseProperties(String authJson, String storeType);


    /**
     * 解析桶相关信息
     *
     * @param authJson 认证JSON字符串
     * @param storeType 存储类型
     * @return 桶信息
     */
    BucketInfo parseBucketInfo(String authJson, String storeType);

    /**
     * 对象是否存在于某个桶中
     *
     * @param authJson 认证JSON字符串
     * @param objectKey 对象键
     * @param storeType 存储类型
     * @return 对象是否存在
     */
    boolean existObject(String authJson, String objectKey,String storeType);

    /**
     * 文件上传
     *
     * @return 上传结果
     */
    String upload();

    /**
     * AES 加密 secretKey
     *
     * @param authJson 认证JSON字符串
     * @param secretKey 密钥
     * @return 加密后的密钥
     */
    String encryptSecretKey(String authJson, String secretKey);
}
