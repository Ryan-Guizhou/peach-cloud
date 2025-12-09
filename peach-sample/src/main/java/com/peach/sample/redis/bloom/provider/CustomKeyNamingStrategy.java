package com.peach.sample.redis.bloom.provider;


import com.peach.redis.bloom.spi.KeyNamingStrategy;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/11/27 15:05
 */
public class CustomKeyNamingStrategy implements KeyNamingStrategy {


    private final String environment;

    public CustomKeyNamingStrategy() {
        this.environment = System.getProperty("app.env",
                System.getenv().getOrDefault("APP_ENV", "dev"));
    }

    public CustomKeyNamingStrategy(String environment) {
        this.environment = environment;
    }

    private String buildEnvKey(String baseKey) {
        return environment + ":" + baseKey;
    }

    @Override
    public String segmentsKey(String prefix, String namespace) {
        String baseKey = prefix + ":" + namespace + ":segments";
        return buildEnvKey(baseKey);
    }

    @Override
    public String lockKey(String prefix, String namespace) {
        String baseKey = prefix + ":" + namespace + ":lock";
        return buildEnvKey(baseKey);
    }

    @Override
    public String segmentName(String prefix, String namespace, int index) {
        String baseKey = prefix + ":" + namespace + ":s" + index;
        return buildEnvKey(baseKey);
    }

    @Override
    public String segmentCountKey(String prefix, String namespace, String segmentName) {
        return segmentName + ":count";
    }

    @Override
    public String capacityMapKey(String prefix, String namespace) {
        String baseKey = prefix + ":" + namespace + ":capacity";
        return buildEnvKey(baseKey);
    }

    @Override
    public String fppMapKey(String prefix, String namespace) {
        String baseKey = prefix + ":" + namespace + ":fpp";
        return buildEnvKey(baseKey);
    }

    @Override
    public String getName() {
        return "environment-aware-naming(env=" + environment + ")";
    }
}
