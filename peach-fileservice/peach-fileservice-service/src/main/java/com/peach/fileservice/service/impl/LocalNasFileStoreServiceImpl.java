package com.peach.fileservice.service.impl;

import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.LocalProperties;
import com.peach.fileservice.config.store.NasProperties;
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
        havingValue = StoreConstants.NAS)
@EnableConfigurationProperties(NasProperties.class)
public class LocalNasFileStoreServiceImpl extends LocalFileStoreServiceImpl {

    public LocalNasFileStoreServiceImpl(NasProperties properties) {
        // NAS is usually mounted as a local directory, so we can reuse LocalFileStoreServiceImpl logic
        super(convertToLocalProperties(properties));
        log.info("NasFileStoreServiceImpl init, rootPath: {}", rootPath);
    }

    private static LocalProperties convertToLocalProperties(NasProperties properties) {
        LocalProperties localProperties = new LocalProperties();
        localProperties.setRootPath(properties.getRootPath());
        localProperties.setPrefix(properties.getPrefix());
        localProperties.setProxyHost(properties.getProxyHost());
        localProperties.setEnableClamav(properties.isEnableClamav());
        return localProperties;
    }
}
