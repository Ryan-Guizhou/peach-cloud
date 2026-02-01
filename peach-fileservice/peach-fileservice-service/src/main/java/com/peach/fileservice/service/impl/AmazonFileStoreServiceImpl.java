package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.AmazonProperties;
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
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:44
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.AMAZON)
@EnableConfigurationProperties(AmazonProperties.class)
public class AmazonFileStoreServiceImpl extends AbstractFileStoreService {

    protected final String bucketName;
    protected final AmazonS3 s3Client;
    protected final String prefix;
    protected final String proxyHost;
    protected final boolean isEnableClamav;

    public AmazonFileStoreServiceImpl(AmazonProperties properties) {
        log.info("AmazonFileStoreServiceImpl init, properties: {}", JSON.toJSONString(properties));
        this.bucketName = properties.getBucketName();
        this.prefix = properties.getPrefix();
        this.proxyHost = properties.getProxyHost();
        this.isEnableClamav = properties.isEnableClamav();

        ClientConfiguration config = new ClientConfiguration();
        config.setProtocol(Protocol.HTTP);
        
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey())))
                .withClientConfiguration(config);

        if (StringUtil.isNotEmpty(properties.getEndpoint())) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(properties.getEndpoint(), properties.getRegion()));
        } else if (StringUtil.isNotEmpty(properties.getRegion())) {
            builder.withRegion(properties.getRegion());
        }

        this.s3Client = builder.build();
        checkInitIsSuccess();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)) return false;
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        try {
            ObjectListing listing = s3Client.listObjects(bucketName, sourceDir);
            do {
                for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                    String key = summary.getKey();
                    if (key.endsWith(PATH_SEPARATOR)) continue;
                    String relativePath = key.substring(sourceDir.length());
                    String targetKey = targetDir + relativePath;
                    s3Client.copyObject(bucketName, key, bucketName, targetKey);
                }
                listing = s3Client.listNextBatchOfObjects(listing);
            } while (listing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl copyDir error", e);
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
            ObjectListing listing = s3Client.listObjects(bucketName, sourceDir);
            do {
                for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                    String key = summary.getKey();
                    if (key.endsWith("/")) continue;
                    String relativePath = key.substring(sourceDir.length());
                    File localFile = new File(baseDir, relativePath);
                    if (!localFile.getParentFile().exists()) localFile.getParentFile().mkdirs();
                    S3Object s3Object = s3Client.getObject(bucketName, key);
                    try (InputStream in = s3Object.getObjectContent()) {
                        FileUtil.writeFromStream(in, localFile);
                    }
                }
                listing = s3Client.listNextBatchOfObjects(listing);
            } while (listing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl downDir error", e);
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
            log.error("AmazonFileStoreServiceImpl upload content error", e);
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
            log.error("AmazonFileStoreServiceImpl download error", e);
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
            return s3Client.getObject(bucketName, normalizePath(key)).getObjectContent();
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl getInputStreamByKey error", e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) return false;
        String keyPath = normalizePath(removeUrlHost(key));
        try {
            ObjectListing listing = s3Client.listObjects(bucketName, keyPath);
            do {
                List<String> keys = listing.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
                if (!keys.isEmpty()) {
                    s3Client.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(keys.toArray(new String[0])));
                }
                listing = s3Client.listNextBatchOfObjects(listing);
            } while (listing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl delete error", e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            s3Client.copyObject(bucketName, normalizePath(currentPath), bucketName, normalizePath(targetPath));
            return true;
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl copyFile error", e);
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
        s3Client.setObjectAcl(bucketName, normalizePath(path), CannedAccessControlList.PublicRead);
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
            s3Client.putObject(bucketName, key, inputStream, null);
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            return removeUrlHost(s3Client.generatePresignedUrl(bucketName, key, expiration).toString());
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl uploadInputStream error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        try {
            if (!s3Client.doesObjectExist(bucketName, keyPath)) return StringUtil.EMPTY;
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String url = s3Client.generatePresignedUrl(bucketName, keyPath, expiration).toString();
            return replaceUrlHost(url, isUrl);
        } catch (Exception e) {
            log.error("AmazonFileStoreServiceImpl getOrgUrlByKey error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected void checkInitIsSuccess() {
        boolean exists = s3Client.doesBucketExistV2(bucketName);
        log.info("AmazonFileStoreServiceImpl init success, bucket: {}, exists: {}", bucketName, exists);
    }
}
