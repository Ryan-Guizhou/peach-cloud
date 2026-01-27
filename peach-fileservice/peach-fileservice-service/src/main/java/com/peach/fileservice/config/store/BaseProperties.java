package com.peach.fileservice.config.store;

import lombok.Data;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 11:30
 */
@Data
public class BaseProperties {

    private String accessKey;

    private String secretKey;

    private String endpoint;

    private String bucketName;

    private boolean isEnableClamav;
}
