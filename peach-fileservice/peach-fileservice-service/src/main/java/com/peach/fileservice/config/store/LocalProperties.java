package com.peach.fileservice.config.store;

import com.peach.fileservice.StoreConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:30
 */
@Data
@ConfigurationProperties(prefix = StoreConstants.LOCAL_CONDITIONAL_PREFIX)
public class LocalProperties extends BaseProperties {
    /**
     * 本地存储根路径 / Local storage root path
     */
    private String rootPath;
}
