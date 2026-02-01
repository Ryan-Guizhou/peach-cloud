package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.BosObject;
import com.baidubce.services.bos.model.BosObjectSummary;
import com.baidubce.services.bos.model.CannedAccessControlList;
import com.baidubce.services.bos.model.ListObjectsRequest;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.google.common.collect.Lists;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.BosProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 21:13
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.BOS)
@EnableConfigurationProperties(BosProperties.class)
public class BosFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;
    
    private final BosClient bosClient;
    
    private final String prefix;
    
    private final String proxyHost;
    
    private final boolean isEnableClamav;

    public BosFileStoreServiceImpl(BosProperties bosProperties) {
        log.info("BosFileStoreServiceImpl init, bosProperties: {}", JSON.toJSONString(bosProperties));
        this.bucketName = bosProperties.getBucketName();
        this.prefix = bosProperties.getPrefix();
        this.proxyHost = bosProperties.getProxyHost();
        this.isEnableClamav = bosProperties.isEnableClamav();

        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(bosProperties.getAccessKey(), bosProperties.getSecretKey()));
        config.setEndpoint(bosProperties.getEndpoint());
        this.bosClient = new BosClient(config);
        checkInitIsSuccess();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)) return false;
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        try {
            String marker = null;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(sourceDir);
                listObjectsRequest.setMaxKeys(MAX_KEYS);
                listObjectsRequest.setMarker(marker);
                ListObjectsResponse response = bosClient.listObjects(listObjectsRequest);
                for (BosObjectSummary summary : response.getContents()) {
                    String key = summary.getKey();
                    if (key.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    String relativePath = key.substring(sourceDir.length());
                    String targetKey = targetDir + relativePath;
                    bosClient.copyObject(bucketName, key, bucketName, targetKey);
                }
                marker = response.getNextMarker();
            } while (marker != null);
            return true;
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl copyDir error", e);
            return false;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(localDir)) return false;
        sourceDir = normalizePath(sourceDir);
        localDir = normalizePath(localDir);
        try {
            File baseDir = new File(localDir);
            if (!baseDir.exists()) baseDir.mkdirs();
            String marker = null;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(sourceDir);
                listObjectsRequest.setMaxKeys(MAX_KEYS);
                listObjectsRequest.setMarker(marker);
                ListObjectsResponse response = bosClient.listObjects(listObjectsRequest);
                for (BosObjectSummary summary : response.getContents()) {
                    String key = summary.getKey();
                    if (key.endsWith(PATH_SEPARATOR)) {
                        continue;
                    }
                    String relativePath = key.substring(sourceDir.length());
                    File localFile = new File(baseDir, relativePath);
                    if (!localFile.getParentFile().exists()) localFile.getParentFile().mkdirs();
                    BosObject bosObject = bosClient.getObject(bucketName, key);
                    try (InputStream in = bosObject.getObjectContent()) {
                        FileUtil.writeFromStream(in, localFile);
                    }
                }
                marker = response.getNextMarker();
            } while (marker != null);
            return true;
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl downDir error", e);
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
            log.error("BosFileStoreServiceImpl upload content error", e);
            return StringUtil.EMPTY;
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
            if (in == null) return false;
            String downloadPath = normalizePath(localPath) + PATH_SEPARATOR + fileName;
            FileUtil.writeFromStream(in, downloadPath);
            return true;
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl download error", e);
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
            return bosClient.getObject(bucketName, normalizePath(key)).getObjectContent();
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl getInputStreamByKey error", e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) return false;
        String keyPath = removeUrlHost(key);
        try {
            bosClient.deleteObject(bucketName, normalizePath(keyPath));
            return true;
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl delete error", e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            bosClient.copyObject(bucketName, normalizePath(currentPath), bucketName, normalizePath(targetPath));
            return true;
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl copyFile error", e);
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
        bosClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
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
        if (checkForClamav(inputStream)){
            return StringUtil.EMPTY;
        }
        try {
            String key = buildPathKey(targetPath, fileName);
            bosClient.putObject(bucketName, key, inputStream);
            // BOS 默认没有直接设置 Object ACL 的便捷方法，通常是在 Bucket 级别设置或通过 API
            URL url = bosClient.generatePresignedUrl(bucketName, key, (int) (EXPIRATION / MAX_KEYS));
            return removeUrlHost(url.toString());
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl uploadInputStream error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        try {
            if (!bosClient.doesObjectExist(bucketName, keyPath)) return StringUtil.EMPTY;
            URL url = bosClient.generatePresignedUrl(bucketName, keyPath, (int) (EXPIRATION / MAX_KEYS));
            return replaceUrlHost(url.toString(), isUrl);
        } catch (Exception e) {
            log.error("BosFileStoreServiceImpl getOrgUrlByKey error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected void checkInitIsSuccess() {
        boolean bucketExist = bosClient.doesBucketExist(bucketName);
        log.info("CosFileStoreServiceImpl init bean,bucketName is: [{}],bucketName isExist: [{}]", bucketName,bucketExist);
    }
}
