package com.peach.fileservice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.obs.services.ObsClient;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.ObsProperties;
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

    public ObsFileStoreServiceImpl(ObsProperties obsProperties) {
        log.info("ObsFileStoreServiceImpl init bean,ObsProperties is: [{}]", JSON.toJSON(obsProperties));
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
        if (isEnableClamav){
            log.info("File virus scanning and compliance verification are in progress ..");
            boolean flag = checkForClamav(inputStream);
            log.info("File virus scanning and compliance verification completed, flag is :[{}]",flag);
            if (flag){
                log.info("File is safe, continue uploading");
            }else {
                log.info("File is not safe, please check");
                return "";
            }
        }
        String resultUrl = null;
        try {
            String pathKey = buildPathKey(targetPath, fileName);
            PutObjectResult result = obsClient.putObject(bucketName, pathKey, inputStream);
            if (null != result) {
                obsClient.setObjectAcl(bucketName, pathKey, AccessControlList.REST_CANNED_PUBLIC_READ);
                // 设置URL过期时间为2年
//                long expiration = System.currentTimeMillis() + EXPIRATION;
                long expiration = System.currentTimeMillis();
                TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
                //设置桶名,一般都是写在配置里，这里直接赋值即可
                request.setBucketName(bucketName);
                //这里相当于设置你上传到obs的文件路
                request.setObjectKey(pathKey);
                TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
                String ossUrl = response.getSignedUrl();
                if (ossUrl.contains("https://")) {
                    resultUrl = ossUrl.replaceAll("https://[^/]+", "");
                } else {
                    resultUrl = ossUrl.replaceAll("http://[^/]+", "");
                }
            }
        }catch (Exception e){
            log.debug("uploadInputStream error:{}",e.getMessage());
        }
        return resultUrl;
    }
}
