package com.peach.fileservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSSClient;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.OssProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:41
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.OSS)
@EnableConfigurationProperties(OssProperties.class)
public class OssFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;

    private final OSSClient ossClient;

    public OssFileStoreServiceImpl(OssProperties ossProperties) {
        log.info("OssFileStoreServiceImpl init,ossProperties is : {}", JSON.toJSON(ossProperties));
        this.bucketName = ossProperties.getBucketName();
        this.ossClient = new OSSClient(ossProperties.getEndpoint(),
                ossProperties.getAccessKey(), ossProperties.getSecretKey());
    }


    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        return false;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        return false;
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return "";
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        return "";
    }

    @Override
    public List<String> upload(File[] file, String targetPath) {
        return Collections.emptyList();
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return "";
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        return false;
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return null;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        return null;
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return "";
    }

    @Override
    public String getPathByKey(String key) {
        return "";
    }

    @Override
    public void setPublicReadAcl(String path) {

    }


    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName){
        ossClient.putObject(bucketName, targetPath + fileName, inputStream);
        return  "";
    }
}
