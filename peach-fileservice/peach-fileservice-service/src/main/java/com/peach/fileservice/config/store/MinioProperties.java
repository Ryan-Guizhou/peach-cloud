package com.peach.fileservice.config.store;

import com.peach.fileservice.StoreConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 11:31
 */
@Data
@ConfigurationProperties(prefix = StoreConstants.MINIO_CONDITIONAL_PREFIX)
public class MinioProperties extends BaseProperties{
}
