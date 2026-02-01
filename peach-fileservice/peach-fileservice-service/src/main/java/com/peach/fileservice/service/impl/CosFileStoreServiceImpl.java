package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.CosProperties;
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
import java.util.Date;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:42
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.COS)
@EnableConfigurationProperties(CosProperties.class)
public class CosFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;

    private final COSClient cosClient;

    private final String prefix;

    private final String proxyHost;

    private final boolean isEnableClamav;

    public CosFileStoreServiceImpl(CosProperties cosProperties) {
        log.info("CosFileStoreServiceImpl init, cosProperties: {}", JSON.toJSONString(cosProperties));
        this.bucketName = cosProperties.getBucketName();
        this.prefix = cosProperties.getPrefix();
        this.proxyHost = cosProperties.getProxyHost();
        this.isEnableClamav = cosProperties.isEnableClamav();
        this.cosClient = new COSClient(new BasicCOSCredentials(cosProperties.getAccessKey(), cosProperties.getSecretKey()),
                new ClientConfig(new Region(cosProperties.getRegion())));
        checkInitIsSuccess();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)) {
            return false;
        }
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        try {
            ObjectListing objectListing = null;
            do {
                objectListing = cosClient.listObjects(bucketName, sourceDir);
                for (COSObjectSummary summary : objectListing.getObjectSummaries()) {
                    String sourceKey = summary.getKey();
                    if (sourceKey.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    String relativePath = sourceKey.substring(sourceDir.length());
                    String targetKey = targetDir + relativePath;
                    cosClient.copyObject(bucketName, sourceKey, bucketName, targetKey);
                }
            } while (objectListing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl copyDir error", e);
            return false;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(localDir)) {
            return false;
        }
        sourceDir = normalizePath(sourceDir);
        localDir = normalizePath(localDir);
        try {
            File baseDir = new File(localDir);
            if (!baseDir.exists()) baseDir.mkdirs();
            ObjectListing objectListing = null;
            do {
                objectListing = cosClient.listObjects(bucketName, sourceDir);
                for (COSObjectSummary summary : objectListing.getObjectSummaries()) {
                    String key = summary.getKey();
                    if (key.endsWith(PATH_SEPARATOR)){
                        continue;
                    }
                    String relativePath = key.substring(sourceDir.length());
                    File localFile = new File(baseDir, relativePath);
                    if (!localFile.getParentFile().exists()) localFile.getParentFile().mkdirs();
                    COSObject cosObject = cosClient.getObject(bucketName, key);
                    try (InputStream in = cosObject.getObjectContent()) {
                        FileUtil.writeFromStream(in, localFile);
                    }
                }
            } while (objectListing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl downDir error", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return uploadInputStream(inputStream, targetPath, fileName);
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl upload content error", e);
            return "";
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null) return Collections.emptyList();
        List<String> list = Lists.newArrayList();
        for (File f : files) {
            list.add(upload(f, targetPath, f.getName()));
        }
        return list;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file, targetPath, fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        try (InputStream in = getInputStreamByKey(targetPath)) {
            if (in == null) {
                return false;
            }
            String downloadPath = normalizePath(localPath) + PATH_SEPARATOR + fileName;
            FileUtil.writeFromStream(in, downloadPath);
            return true;
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl download error", e);
            return false;
        }
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return getInputStreamByKey(buildPathKey(targetPath, fileName));
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        try {
            return cosClient.getObject(bucketName, normalizePath(key)).getObjectContent();
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl getInputStreamByKey error", e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) return false;
        String keyPath = removeUrlHost(key);
        try {
            cosClient.deleteObject(bucketName, normalizePath(keyPath));
            return true;
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl delete error", e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            cosClient.copyObject(bucketName, normalizePath(currentPath), bucketName, normalizePath(targetPath));
            return true;
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl copyFile error", e);
            return false;
        }
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key, true);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key, false);
    }

    @Override
    public void setPublicReadAcl(String path) {
        try {
            cosClient.setObjectAcl(bucketName, normalizePath(path), CannedAccessControlList.PublicRead);
        }catch (Exception e){
            log.error("CosFileStoreServiceImpl setPublicReadAcl error", e);
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
        if (isEnableClamav && !checkForClamav(inputStream)) return "";
        try {
            String key = buildPathKey(targetPath, fileName);
            cosClient.putObject(bucketName, key, inputStream, null);
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            return removeUrlHost(cosClient.generatePresignedUrl(bucketName, key, expiration, HttpMethodName.GET).toString());
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl uploadInputStream error", e);
            return "";
        }
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        try {
            if (!cosClient.doesObjectExist(bucketName, keyPath)) return "";
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String url = cosClient.generatePresignedUrl(bucketName, keyPath, expiration, HttpMethodName.GET).toString();
            return replaceUrlHost(url, isUrl);
        } catch (Exception e) {
            log.error("CosFileStoreServiceImpl getOrgUrlByKey error", e);
            return "";
        }
    }

    @Override
    protected void checkInitIsSuccess() {
        boolean bucketExist = cosClient.doesBucketExist(bucketName);
        log.info("CosFileStoreServiceImpl init bean,bucketName is: [{}],bucketName isExist: [{}]", bucketName,bucketExist);
    }
}
