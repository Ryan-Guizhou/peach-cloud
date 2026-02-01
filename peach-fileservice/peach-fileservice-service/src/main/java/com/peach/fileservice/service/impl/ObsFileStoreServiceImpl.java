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
        this.prefix = obsProperties.getPrefix();
        this.proxyHost = obsProperties.getProxyHost();
        this.bucketName = obsProperties.getBucketName();
        this.isEnableClamav = obsProperties.isEnableClamav();
        this.obsClient = new ObsClient(obsProperties.getAccessKey(), obsProperties.getSecretKey(), obsProperties.getEndpoint());
        checkInitIsSuccess();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)) {
            log.error("ObsFileStoreServiceImpl copyDir error:sourceDir or targetDir is null");
            return false;
        }
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        String marker = null;
        try {
            do {
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setPrefix(sourceDir);
                request.setMarker(marker);
                request.setMaxKeys(MAX_KEYS);

                ObjectListing listing = obsClient.listObjects(request);

                for (ObsObject obsObject : listing.getObjects()) {
                    String sourceKey = obsObject.getObjectKey();
                    if (sourceKey.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    String relativePath = sourceKey.substring(sourceDir.length());
                    String targetKey = targetDir + relativePath;
                    obsClient.copyObject(bucketName, sourceKey, bucketName, targetKey);
                }
                marker = listing.getNextMarker();
            } while (marker != null);
            return true;
        } catch (Exception e) {
            log.error("ObsFileStoreServiceImpl copyDir error:{}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(localDir)) {
            log.error("ObsFileStoreServiceImpl downDir error:sourceDir or localDir is null");
            return false;
        }
        sourceDir = normalizePath(sourceDir);
        localDir = normalizePath(localDir);
        try {
            File baseDir = new File(localDir);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new RuntimeException("Cannot create local dir: " + localDir);
            }
            String marker = null;
            do {
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setPrefix(sourceDir);
                request.setMarker(marker);
                request.setMaxKeys(MAX_KEYS);

                ObjectListing listing = obsClient.listObjects(request);
                for (ObsObject obsObject : listing.getObjects()) {
                    String objectKey = obsObject.getObjectKey();
                    if (objectKey.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    String relativePath = objectKey.substring(sourceDir.length());
                    File localFile = new File(baseDir, relativePath);
                    File parentDir = localFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new RuntimeException("Cannot create dir: " + parentDir);
                    }
                    ObsObject object = obsClient.getObject(bucketName, objectKey);
                    try (InputStream in = object.getObjectContent()) {
                        FileUtil.writeFromStream(in, localFile);
                    }
                }
                marker = listing.getNextMarker();
            } while (marker != null);
            return true;
        } catch (Exception e) {
            log.error("ObsFileStoreServiceImpl downDir error:{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return uploadInputStream(inputStream, targetPath, fileName);
        } catch (Exception e) {
            log.error("uploadInputStream error:{}", e.getMessage());
            return StringUtil.EMPTY;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File file : files) {
            String url = upload(file, targetPath, FileUtil.getName(file));
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
            localPath = normalizePath(localPath);
            String downloadPath = localPath.endsWith(PATH_SEPARATOR) ? localPath + fileName : localPath + PATH_SEPARATOR + fileName;
            if (inputStream != null) {
                FileUtil.writeFromStream(inputStream, downloadPath);
            }
            return FileUtil.exist(downloadPath);
        } catch (Exception e) {
            log.error("obsStorageImpl download file error！", e);
            return Boolean.FALSE;
        }
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        try {
            String fileKeyPath = buildPathKey(targetPath, fileName);
            return obsClient.getObject(bucketName, fileKeyPath).getObjectContent();
        } catch (Exception e) {
            log.error("Failed to retrieve the file:[{}]", targetPath, e);
            return null;
        }
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        try {
            return obsClient.getObject(bucketName, normalizePath(key)).getObjectContent();
        } catch (Exception e) {
            log.error("Failed to retrieve the file:[{}]", key, e);
        }
        return null;
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) {
            log.error("delete key contains illegal characters, key: {}", key);
            return Boolean.FALSE;
        }
        String fileKeyPath = removeUrlHost(key);
        String nextMarker = null;
        ObjectListing objectListing;
        Boolean isDelete = Boolean.TRUE;
        try {
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
                listObjectsRequest.setBucketName(bucketName);
                listObjectsRequest.setMarker(nextMarker);
                listObjectsRequest.setPrefix(normalizePath(fileKeyPath));
                objectListing = obsClient.listObjects(listObjectsRequest);
                if (!objectListing.getObjects().isEmpty()) {
                    DeleteObjectsRequest deleteObjectRequest = new DeleteObjectsRequest();
                    deleteObjectRequest.setBucketName(bucketName);
                    for (ObsObject object : objectListing.getObjects()) {
                        deleteObjectRequest.addKeyAndVersion(object.getObjectKey());
                    }
                    obsClient.deleteObjects(deleteObjectRequest);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            log.error("Traverse and delete error, path is:[{}]", key, e);
            isDelete = Boolean.FALSE;
        }
        return isDelete;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        if (StringUtil.isEmpty(currentPath) || StringUtil.isEmpty(targetPath)) {
            log.error("currentPath or targetPath is empty");
            return Boolean.FALSE;
        }
        currentPath = normalizePath(currentPath);
        targetPath = normalizePath(targetPath);
        if (currentPath.equals(targetPath)) {
            log.error("currentPath is equal to targetPath");
            return Boolean.TRUE;
        }
        try {
            obsClient.copyObject(bucketName, currentPath, bucketName, targetPath);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("copy file failed,currentPath is [{}] targetPath is [{}]", currentPath, targetPath, e);
            return Boolean.FALSE;
        }
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key, Boolean.TRUE);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key, Boolean.FALSE);
    }

    @Override
    public void setPublicReadAcl(String path) {
        try {
            obsClient.setObjectAcl(bucketName, normalizePath(path), AccessControlList.REST_CANNED_PUBLIC_READ);
        } catch (Exception e) {
            log.error("ObsFileStoreServiceImpl setPublicReadAcl field" + e.getMessage(), e);
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
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String resultUrl = StringUtil.EMPTY;
        if (checkForClamav(inputStream)) {
            return resultUrl;
        }
        try {
            String pathKey = buildPathKey(targetPath, fileName);
            obsClient.putObject(bucketName, pathKey, inputStream);
            obsClient.setObjectAcl(bucketName, pathKey, AccessControlList.REST_CANNED_PUBLIC_READ);
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            request.setBucketName(bucketName);
            request.setObjectKey(pathKey);
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            resultUrl = removeUrlHost(response.getSignedUrl());
        } catch (Exception e) {
            log.error("uploadInputStream error:{}", e.getMessage(), e);
        }
        return resultUrl;
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        String url = StringUtil.EMPTY;
        try {
            boolean flag = obsClient.doesObjectExist(bucketName, keyPath);
            if (!flag) {
                log.error("The file does not exist!");
                return url;
            }
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            request.setBucketName(bucketName);
            request.setObjectKey(keyPath);
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            url = replaceUrlHost(response.getSignedUrl(), isUrl);
        } catch (Exception e) {
            log.error("obsStorageImpl getUrlByKey failed" + e.getMessage(), e);
        }
        return url;
    }

    @Override
    protected void checkInitIsSuccess() {
        boolean bucketExist = obsClient.headBucket(bucketName);
        log.info("ObsFileStoreServiceImpl init bean,bucketName is: [{}],bucketName isExist: [{}]", bucketName,bucketExist);
    }
}
