package com.peach.fileservice.config.store;

import com.peach.fileservice.StoreConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 23:35
 */
@Data
@ConfigurationProperties(prefix = StoreConstants.BOS_CONDITIONAL_PREFIX)
public class BosProperties extends BaseProperties{

}
