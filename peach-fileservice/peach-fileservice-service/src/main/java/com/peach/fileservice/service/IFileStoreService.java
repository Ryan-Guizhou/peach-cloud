package com.peach.fileservice.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/25 17:36
 * @Description 通用的文件服务能力
 */
public interface IFileStoreService {

    /**
     * 复制文件夹以及文件夹下面所有内容
     *
     * @param sourceDir 源文件夹
     * @param targetDir 目标目录
     * @return boolean
     */
    boolean copyDir(String sourceDir, String targetDir);

    /**
     * 下载某个文件夹下所有的文件
     *
     * @param sourceDir 资源文件夹
     * @param localDir  本地文件夹
     * @return boolean
     */
    boolean downDir(String sourceDir, String localDir);

    /**
     * 文件上传接口
     *
     * @param inputStream 文件流
     * @param targetPath  目标上传路径
     * @param fileName    文件名称 带后缀
     * @return: java.lang.String 带签名存储路径
     * @author: pc
     */
    String upload(InputStream inputStream, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param content    文件内容
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     * @author: pc
     */
    String upload(String content, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param file       文件，一个或者多个
     * @param targetPath 目标上传路径
     * @return: java.util.List  带签名存储路径
     * @author: pc
     */
    List<String> upload(File[] file, String targetPath);

    /**
     * 文件上传接口
     *
     * @param file       文件
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     */
    String upload(File file, String targetPath, String fileName);


    /**
     * 从 targetPath 下载文件到本地指定目录
     *
     * @param targetPath 要下载的目标资源路径,/data/file/test/6.jpg?Expires=1803807824&OSSAccessKeyId=LTAI5tQiXiDypb3HDKq2uGKi&Signature=qDHJTu3G441I6WvId6RKHOdhvEs%3D
     * @param localPath 本地的存储路径
     * @param fileName 下载之后的文件名
     * @return: void
     * @author: pc
     */
    boolean download(String targetPath, String localPath, String fileName);


    /**
     * 通过文件路径获取 文件流
     *
     * @param targetPath （样例：/data/nfsdata/ 或者 /data/nfsdata）
     * @param fileName   (样例：test.yml)
     * @return: java.io.InputStream
     * @author: pc
     */
    InputStream getInputStream(String targetPath, String fileName);


    /**
     * 通过文件路径获取 文件流(文件全路径)
     *
     * @param key (样例：/nacos/config/test.yml)
     * @return
     */
    InputStream getInputStreamByKey(String key);

    /**
     * 删除文件或者文件夹
     *
     * @param key 目标地址 (样例：/nacos/config/test.yml 或者 /nacos/config/ )
     * @return
     */
    boolean delete(String key);


    /**
     * 复制文件
     *
     * @param currentPath
     * @param targetPath
     * @return
     */
    boolean copyFile(String currentPath, String targetPath);


    /**
     * 通过文件路径+文件名 获取url（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    String getUrlByKey(String key);

    /**
     * 通过文件路径+文件名 获取带签名路径（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    String getPathByKey(String key);

    /**
     * 为文件设置公共读 针对oss、ceph 等分布式存储本
     *
     * @param path
     * @return
     */
    void setPublicReadAcl(String path);

}
