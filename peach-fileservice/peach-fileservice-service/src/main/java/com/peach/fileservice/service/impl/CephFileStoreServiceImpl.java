package com.peach.fileservice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.AmazonProperties;
import com.peach.fileservice.config.store.CephProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

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
        havingValue = StoreConstants.CEPH)
@EnableConfigurationProperties(CephProperties.class)
public class CephFileStoreServiceImpl extends AmazonFileStoreServiceImpl {

    public CephFileStoreServiceImpl(CephProperties properties) {
        // Reuse Amazon implementation but with Ceph properties
        super(convertToAmazonProperties(properties));
        log.info("CephFileStoreServiceImpl init, properties: {}", JSON.toJSONString(properties));
    }

    private static AmazonProperties convertToAmazonProperties(CephProperties properties) {
        AmazonProperties amazonProperties = new AmazonProperties();
        amazonProperties.setAccessKey(properties.getAccessKey());
        amazonProperties.setSecretKey(properties.getSecretKey());
        amazonProperties.setEndpoint(properties.getEndpoint());
        amazonProperties.setBucketName(properties.getBucketName());
        amazonProperties.setRegion(properties.getRegion());
        amazonProperties.setPrefix(properties.getPrefix());
        amazonProperties.setProxyHost(properties.getProxyHost());
        amazonProperties.setEnableClamav(properties.isEnableClamav());
        return amazonProperties;
    }
}
