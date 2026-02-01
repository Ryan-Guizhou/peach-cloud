package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.AccessControlList;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectResult;
import com.google.common.collect.Lists;
import com.obs.services.model.TemporarySignatureRequest;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.OssProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.K;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
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
        havingValue = StoreConstants.OSS,
        matchIfMissing = true)
@EnableConfigurationProperties(OssProperties.class)
public class OssFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;

    private final OSSClient ossClient;

    private final String proxyHost;

    private String prefix;

    private boolean isEnableClamav;

    public OssFileStoreServiceImpl(OssProperties ossProperties) {
        log.info("OssFileStoreServiceImpl init,ossProperties is : {}", JSON.toJSON(ossProperties));
        this.prefix = ossProperties.getPrefix();
        this.proxyHost = ossProperties.getProxyHost();
        this.isEnableClamav = ossProperties.isEnableClamav();
        this.bucketName = ossProperties.getBucketName();
        this.ossClient = new OSSClient(ossProperties.getEndpoint(),
                ossProperties.getAccessKey(), ossProperties.getSecretKey());
        checkInitIsSuccess();
    }


    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)){
            log.error("OssFileStoreServiceImpl copyDir error:sourceDir or targetDir is null");
            return false;
        }
        // 1. 目录规范化
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        String marker = null;
        try {
            do {
                // 2. 构造 V1 ListObjects 请求
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setPrefix(sourceDir);
                request.setMarker(marker);
                request.setMaxKeys(MAX_KEYS);

                ObjectListing listing = ossClient.listObjects(request);

                // 3. 遍历所有 object
                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    String sourceKey = summary.getKey();

                    // 跳过目录占位符
                    if (sourceKey.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    // 4. 计算相对路径
                    String relativePath = sourceKey.substring(sourceDir.length());
                    String targetKey = targetDir + relativePath;

                    // 5. OSS 服务端拷贝
                    ossClient.copyObject(bucketName, sourceKey, bucketName, targetKey);
                }

                // 6. 是否还有下一页
                marker = listing.getNextMarker();

            } while (marker != null);
            return true;
        } catch (Exception e) {
            log.error("OssFileStoreServiceImpl copyDir error:{}",e.getMessage());
            return false;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(localDir)){
            log.error("OssFileStoreServiceImpl copyDir error:sourceDir or localDir is null");
            return false;
        }
        // 1. 目录规范化
        sourceDir = normalizePath(sourceDir);
        localDir = normalizePath(localDir);
        try {

            File baseDir = new File(localDir);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new RuntimeException("Cannot create local dir: " + localDir);
            }

            String marker = null;

            do {
                // 1. 构造 V1 ListObjects 请求
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setPrefix(sourceDir);
                request.setMarker(marker);
                request.setMaxKeys(MAX_KEYS);

                ObjectListing listing = ossClient.listObjects(request);

                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    String objectKey = summary.getKey();

                    // 跳过目录占位符
                    if (objectKey.endsWith("/")) {
                        continue;
                    }

                    // 2. 计算相对路径
                    String relativePath = objectKey.substring(sourceDir.length());
                    File localFile = new File(baseDir, relativePath);

                    // 3. 创建父目录
                    File parentDir = localFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new RuntimeException("Cannot create dir: " + parentDir);
                    }

                    // 4. 下载并写入文件
                    OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
                    try (InputStream in = ossObject.getObjectContent()) {
                        FileUtil.writeFromStream(in, localFile);
                    }
                }

                // 5. 下一页
                marker = listing.getNextMarker();

            } while (marker != null);
            return true;
        } catch (Exception e) {
           log.error("OssFileStoreServiceImpl downDir error:{}",e.getMessage());
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        String resultUrl = StringUtil.EMPTY;
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))){
             resultUrl = uploadInputStream(inputStream,targetPath,fileName);
        }catch (Exception ex){
            log.error("OssFileStoreServiceImpl upload content error:{}",ex.getMessage());
        }
        return resultUrl;
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null){
            return Lists.newArrayList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File file : files) {
            String uploadUrl = upload(file, targetPath, FileUtil.getName(file));
            urlList.add(uploadUrl);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file, targetPath, fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        File file = null;
        try (InputStream inputStream = getInputStreamByKey(targetPath)){
            localPath = normalizePath(localPath);
            String downloadPath = localPath.endsWith(PATH_SEPARATOR) ? localPath + fileName : localPath + PATH_SEPARATOR + fileName;
            if (inputStream != null){
                file = FileUtil.writeFromStream(inputStream, new File(downloadPath));
            }
        }catch (Exception e){
            log.error("OssFileStoreServiceImpl download error:{}",e.getMessage());
        }
        return FileUtil.exist(file);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        InputStream inputStream = null;
        try {
            String keyPath = buildPathKey(targetPath, fileName);
            inputStream = ossClient.getObject(bucketName, keyPath).getObjectContent();
        }catch (Exception e){
            log.error("OssFileStoreServiceImpl getInputStream error:{}",e.getMessage());
        }
        return inputStream;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        try {
            inputStream = ossClient.getObject(bucketName, normalizePath(key)).getObjectContent();
        }catch (Exception e){
            log.error("OssFileStoreServiceImpl getInputStreamByKey error:{}",e.getMessage());
        }
        return inputStream;
    }

    @Override
    public boolean delete(String key) {
        boolean flag = Boolean.FALSE;
        // 判断是否包含特殊字符，如果包含特殊字符不能删除
        boolean hasIllegalChar = isHasIllegalChar(key);
        if (hasIllegalChar){
            log.error("delete file failed,[{}] is illegal,can't be deleted", key);
            return flag;
        }
        String deleteKey = StringUtil.EMPTY;
        try {
            deleteKey = removeUrlHost(key);
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                        .withPrefix(normalizePath(deleteKey))
                        .withMarker(nextMarker);
                objectListing = ossClient.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()){
                    List<String> keys = Lists.newArrayList();
                    for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        keys.add(objectSummary.getKey());
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                            .withKeys(keys);
                    ossClient.deleteObjects(deleteObjectsRequest);
                }
                nextMarker = objectListing.getNextMarker();
            }while (objectListing.isTruncated());
            flag = Boolean.TRUE;
        }catch (Exception e){
            log.error("delete file failed,key is [{}]", deleteKey, e);
            flag = Boolean.FALSE;
        }
        return flag;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        if (StringUtil.isEmpty(currentPath) || StringUtil.isEmpty(targetPath)){
            log.error("currentPath or targetPath is empty");
            return Boolean.FALSE;
        }
        currentPath = normalizePath(currentPath);
        targetPath = normalizePath(targetPath);
        if (currentPath.equals(targetPath)){
            log.error("currentPath is equal to targetPath");
            return Boolean.TRUE;
        }
        try {
            ossClient.copyObject(bucketName, currentPath, bucketName, targetPath);
            return Boolean.TRUE;
        }catch (Exception e){
            log.error("copy file failed,currentPath is [{}] targetPath is [{}]",currentPath,targetPath,e);
            return Boolean.FALSE;
        }
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key,true);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key,false);
    }

    @Override
    public void setPublicReadAcl(String path) {
        try {
            ossClient.setObjectAcl(bucketName, normalizePath(path), CannedAccessControlList.PublicRead);
        }catch (Exception e){
            log.error("setPublicReadAcl error,path is {}",path,e);
        }
    }

    @Override
    protected String prefix() {
        return prefix;
    }

    @Override
    protected String proxyHost() {
        return proxyHost;
    }

    @Override
    protected boolean isClamavEnable() {
        return isEnableClamav;
    }

    @Override
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName){
        String resultUrl = StringUtil.EMPTY;
        if (checkForClamav(inputStream)){
            return resultUrl;
        }
        String keyPath = StringUtil.EMPTY;
        try {
            keyPath = buildPathKey(targetPath,fileName);
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, keyPath, inputStream);
            if (putObjectResult == null){
                log.error("uploadInputStream error,keyPath is {}",keyPath);
                return resultUrl;
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String url = ossClient.generatePresignedUrl(bucketName, keyPath, expiration).toString();
            resultUrl = removeUrlHost( url);
        }catch (Exception e){
            log.error("uploadInputStream error,keyPath is {},e is {}",keyPath,e);
        }
        return  resultUrl;
    }

    @Override
    protected String getOrgUrlByKey(String key,boolean isUrl){
        String keyPath = normalizePath(key);
        String resultUrl = StringUtil.EMPTY;
        try {
            boolean flag = ossClient.doesObjectExist(bucketName, keyPath);
            if(!flag){
                log.error("getOrgUrlByKey error,keyPath is {}",keyPath);
                return resultUrl;
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String url = ossClient.generatePresignedUrl(bucketName, keyPath, expiration).toString();
            resultUrl = replaceUrlHost(url,isUrl);
        } catch (Exception e) {
            log.error("getOrgUrlByKey error,keyPath is {},e is {}",keyPath,e);
        }
        return resultUrl;
    }

    protected void checkInitIsSuccess(){
        boolean bucketExist = ossClient.doesBucketExist(bucketName);
        log.info("OssFileStoreServiceImpl init bean,bucketName is: [{}],bucketName isExist: [{}]", bucketName,bucketExist);
    }
}
