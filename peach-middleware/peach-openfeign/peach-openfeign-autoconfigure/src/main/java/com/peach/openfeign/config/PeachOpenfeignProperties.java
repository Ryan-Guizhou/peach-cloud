package com.peach.openfeign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/2 14:12
 */
@Data
@ConfigurationProperties(prefix = "peach.openfeign")
public class PeachOpenfeignProperties {

    private boolean enabled = true;

    private boolean sameTokenEnabled = true;

    private boolean relayHeaders = true;

    private List<String> excludeHeaders = Arrays.asList(
            "content-type",
            "content-length",
            "host",
            "connection",
            "keep-alive",
            "proxy-connection",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "accept-encoding"
    );
}
