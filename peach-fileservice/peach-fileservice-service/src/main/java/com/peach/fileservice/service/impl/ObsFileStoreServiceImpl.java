package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.obs.services.ObsClient;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.ObsProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:40
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.OBS)
@EnableConfigurationProperties(ObsProperties.class)
public class ObsFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;

    private final ObsClient obsClient;

    private final boolean isEnableClamav;

    private final String prefix;

    private final String proxyHost;

    public ObsFileStoreServiceImpl(ObsProperties obsProperties) {
        log.info("ObsFileStoreServiceImpl init bean,ObsProperties is: [{}]", JSON.toJSON(obsProperties));
        prefix = obsProperties.getPrefix();
        proxyHost = obsProperties.getProxyHost();
        bucketName = obsProperties.getBucketName();
        isEnableClamav = obsProperties.isEnableClamav();
        obsClient = new ObsClient(obsProperties.getAccessKey(), obsProperties.getSecretKey(), obsProperties.getEndpoint());
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
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))){
            return uploadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.error("uploadInputStream error:{}",e.getMessage());
            return StringUtil.EMPTY;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null){
            return Collections.emptyList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File file : files) {
            String url = upload(file,targetPath, FileUtil.getName(file));
            urlList.add(url);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file, targetPath, fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        try (InputStream inputStream = this.getInputStreamByKey(targetPath)) {
            String finalPath = buildPathKey(localPath, fileName);
            if (inputStream != null) {
                FileUtil.writeFromStream(inputStream, finalPath);
            }
            return FileUtil.exist(finalPath);
        } catch (Exception e) {
            log.error("obsStorageImpl download file error！", e);
            return Boolean.FALSE;
        }
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
       try {
           targetPath = normalizePath(targetPath);
           String fileKeyPath = buildPathKey(targetPath, fileName);
           return obsClient.getObject(bucketName, fileKeyPath).getObjectContent();
       }catch (Exception e){
           log.error("Failed to retrieve the file:[{}]",targetPath,e);
           return null;
       }
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        String obsKey = key;
        InputStream inputStream = null;
        try {
            inputStream = obsClient.getObject(bucketName, normalizePath(obsKey)).getObjectContent();
            return inputStream;
        } catch (Exception e) {
            log.error("Failed to retrieve the file:[{}]",obsKey);
            obsKey = obsKey.replace(bucketName + "/", "");
            try {
                log.info("bucketName:[{}],replace key:[{}]",bucketName,obsKey);
                inputStream = obsClient.getObject(bucketName, obsKey).getObjectContent();
                return inputStream;
            } catch (Exception ex) {
                log.error("Failed to retrieve the file after modifying the key:[{}] ",obsKey, e);
            }
        }
        return null;
    }

    @Override
    public boolean delete(String key) {
        if (!isHasIllegalChar(key)){
            log.error("delete key contains illegal characters, key: {}", key);
            return Boolean.FALSE;
        }
        String fileKeyPath = removeUrlHost(key);
        String nextMarker = null;
        ObjectListing objectListing;
        Boolean isDelete = Boolean.TRUE;
        try {
            do{
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
                listObjectsRequest.setBucketName(bucketName);
                listObjectsRequest.setMarker(nextMarker);
                listObjectsRequest.setPrefix(normalizePath(fileKeyPath));
                objectListing = obsClient.listObjects(listObjectsRequest);
                // 遍历删除 / Traverse and delete
                if (!objectListing.getObjects().isEmpty()) {
                    DeleteObjectsRequest deleteObjectRequest = new DeleteObjectsRequest();
                    List<ObsObject> objects = objectListing.getObjects();
                    for (ObsObject object : objects) {
                        deleteObjectRequest.addKeyAndVersion(object.getObjectKey());
                    }
                    deleteObjectRequest.setEncodingType("url");
                    obsClient.deleteObjects(deleteObjectRequest);
                }
                nextMarker = objectListing.getNextMarker();
            }while (objectListing.isTruncated());
        }catch (Exception e){
            log.error("Traverse and delete error, path is:[{}]",key);
            isDelete = Boolean.FALSE;
        }
        return isDelete;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key,Boolean.TRUE);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key,Boolean.FALSE);
    }

    @Override
    public void setPublicReadAcl(String path) {
        try {
            obsClient.setObjectAcl(bucketName, normalizePath(path), AccessControlList.REST_CANNED_PUBLIC_READ);
        }catch (Exception e){
            log.error("ObsFileStoreServiceImpl setPublicReadAcl field"+e.getMessage(),e);
        }
    }

    @Override
    protected String prefix() {
        return prefix;
    }

    @Override
    public String proxyHost() {
        return proxyHost;
    }

    @Override
    protected boolean isClamavEnable() {
        return isEnableClamav;
    }

    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName){
        String resultUrl = StringUtil.EMPTY;
        if (isEnableClamav){
            log.info("File virus scanning and compliance verification are in progress ..");
            boolean flag = checkForClamav(inputStream);
            log.info("File virus scanning and compliance verification completed, flag is :[{}]",flag);
            if (flag){
                log.info("File is safe, continue uploading");
            }else {
                log.info("File is not safe, please check");
                return resultUrl;
            }
        }
        try {
            String pathKey = buildPathKey(targetPath, fileName);
            PutObjectResult result = obsClient.putObject(bucketName, pathKey, inputStream);
            if (result == null) {
                log.error("uploadInputStream result is null");
                return  "";
            }
            obsClient.setObjectAcl(bucketName, pathKey, AccessControlList.REST_CANNED_PUBLIC_READ);
            // 设置URL过期时间为2年
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            //设置桶名,一般都是写在配置里，这里直接赋值即可
            request.setBucketName(bucketName);
            //这里相当于设置你上传到obs的文件路
            request.setObjectKey(pathKey);
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            String signedUrl = response.getSignedUrl();
            resultUrl = removeUrlHost(signedUrl);
        }catch (Exception e){
            log.debug("uploadInputStream error:{}",e.getMessage());
        }
        return resultUrl;
    }


    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        String url = StringUtil.EMPTY;
        try {
            boolean flag = obsClient.doesObjectExist(bucketName, keyPath);
            if (!flag) {
                log.error("文件不存在!");
                return url;
            }
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            //设置桶名,一般都是写在配置里，这里直接赋值即可
            request.setBucketName(bucketName);
            //这里相当于设置你上传到obs的文件路
            request.setObjectKey(keyPath);
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            String obsUrl = response.getSignedUrl();
            url = replaceUrlHost(obsUrl,isUrl);
        } catch (Exception e) {
            log.error("obsStorageImpl getUrlByKey failed"+e.getMessage(), e);
        }
        return url;
    }
}
