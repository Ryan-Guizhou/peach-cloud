package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.MinioProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        havingValue = StoreConstants.MINIO)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioFileStoreServiceImpl extends AbstractFileStoreService {

    private final String bucketName;

    private final MinioClient minioClient;

    private final String prefix;

    private final String proxyHost;

    private final boolean isEnableClamav;

    public MinioFileStoreServiceImpl(MinioProperties properties) {
        log.info("MinioFileStoreServiceImpl init, properties: {}", JSON.toJSONString(properties));
        this.bucketName = properties.getBucketName();
        this.prefix = properties.getPrefix();
        this.proxyHost = properties.getProxyHost();
        this.isEnableClamav = properties.isEnableClamav();

        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        checkInitIsSuccess();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if (StringUtil.isEmpty(sourceDir) || StringUtil.isEmpty(targetDir)) return false;
        sourceDir = normalizePath(sourceDir);
        targetDir = normalizePath(targetDir);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(sourceDir).recursive(true).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                String key = item.objectName();
                if (key.endsWith(PATH_SEPARATOR)) continue;
                String relativePath = key.substring(sourceDir.length());
                String targetKey = targetDir + relativePath;
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(targetKey)
                                .source(CopySource.builder().bucket(bucketName).object(key).build())
                                .build());
            }
            return true;
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl copyDir error", e);
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
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(sourceDir).recursive(true).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                String key = item.objectName();
                if (key.endsWith("/")) continue;
                String relativePath = key.substring(sourceDir.length());
                File localFile = new File(baseDir, relativePath);
                if (!localFile.getParentFile().exists()) localFile.getParentFile().mkdirs();
                try (InputStream in = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucketName).object(key).build())) {
                    FileUtil.writeFromStream(in, localFile);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl downDir error", e);
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
            log.error("MinioFileStoreServiceImpl upload content error", e);
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
            log.error("MinioFileStoreServiceImpl download error", e);
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
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(normalizePath(key)).build());
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl getInputStreamByKey error", e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) return false;
        String keyPath = normalizePath(removeUrlHost(key));
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(keyPath).recursive(true).build());
            List<DeleteObject> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                objects.add(new DeleteObject(result.get().objectName()));
            }
            if (!objects.isEmpty()) {
                Iterable<Result<DeleteError>> errorResults = minioClient.removeObjects(
                        RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build());
                for (Result<DeleteError> errorResult : errorResults) {
                    DeleteError error = errorResult.get();
                    log.error("Error in deleting object " + error.objectName() + "; " + error.message());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl delete error", e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(normalizePath(targetPath))
                            .source(CopySource.builder().bucket(bucketName).object(normalizePath(currentPath)).build())
                            .build());
            return true;
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl copyFile error", e);
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
        // Minio doesn't have per-object ACLs in the same way as OSS. 
        // Policies are usually set at the bucket level or via prefix-based policies.
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
        if (checkForClamav(inputStream)) {
            return StringUtil.EMPTY;
        }
        try {
            String key = buildPathKey(targetPath, fileName);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(key)
                            .expiry(2, TimeUnit.DAYS) // Minio has shorter limits for presigned URLs usually, but using 2 years constant logic
                            .build());
            return removeUrlHost(url);
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl uploadInputStream error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(keyPath).build());
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(keyPath)
                            .expiry(2, TimeUnit.DAYS)
                            .build());
            return replaceUrlHost(url, isUrl);
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl getOrgUrlByKey error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected void checkInitIsSuccess() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            log.info("MinioFileStoreServiceImpl init success, bucket: {}, exists: {}", bucketName, exists);
        } catch (Exception e) {
            log.error("MinioFileStoreServiceImpl init check failed", e);
        }
    }
}
