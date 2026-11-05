package com.peach.gateway.core.ratelimit.model;

/**
 * 网关支持的限流算法。
 */
public enum RateLimitAlgorithm {

    FIXED_WINDOW("fixed-window.lua"),
    SLIDING_WINDOW_LOG("sliding-window-log.lua"),
    SLIDING_WINDOW_COUNTER("sliding-window-counter.lua"),
    TOKEN_BUCKET("token-bucket.lua"),
    LEAKY_BUCKET("leaky-bucket.lua"),
    GCRA("gcra.lua");

    private final String scriptFileName;

    RateLimitAlgorithm(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public String getConfigKey() {
        return name().toLowerCase().replace('_', '-');
    }
}
