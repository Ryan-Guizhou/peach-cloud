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
@ConfigurationProperties(prefix = StoreConstants.NAS_CONDITIONAL_PREFIX)
public class NasProperties extends BaseProperties {
    /**
     * NAS 挂载根路径 / NAS mount root path
     */
    private String rootPath;
}
