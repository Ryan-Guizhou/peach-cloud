package com.peach.redis.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 独立 Jedis Redis 底座配置；兼容历史 peach.redis.* 拓扑参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Data
@ConfigurationProperties(prefix = "peach.redis")
public class PeachRedisProperties {
    private boolean enabled = true;
    private String mode = "standalone";
    private String host;
    private String password = "";
    private String username;
    private String sentinelMaster = "master";
    private String sentinelUsername;
    private String sentinelPassword;
    private int database = 0;
    private boolean sslEnabled = false;
    private JedisPool jedis = new JedisPool();

    /**
     * Jedis pool limits. Defaults are conservative; adjust using Redis connection budget.
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/10
     */
    @Data
    public static class JedisPool {
        private int maxTotal = 128;
        private int minIdle = 1;
        private int maxIdle = 10;
        private long maxWaitMillis = 60000L;
    }
}
