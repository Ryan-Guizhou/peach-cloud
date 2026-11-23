package com.peach.code.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务编码生成器配置。
 *
 * <p>唯一发号权威为 MySQL。Redis 配置控制行锁内的水位同步；
 * 不能用来切换发号权威。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@ConfigurationProperties(prefix = "peach.code")
public class CodeProperties {

    /** 是否启用业务编码自动配置。 */
    private boolean enabled = true;

    /** 是否启用可选 Redis 水位同步。 */

    private boolean redisEnabled = true;

    /** Redis 水位命名空间；与历史前缀发号键分离，不设置 TTL。 */

    private String redisKeyPrefix = "peach:code:sequence:v2:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return Redis 水位同步开关。 */

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    /** @param redisEnabled 是否启用 Redis 水位同步。 */

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    /** @return Redis 水位命名空间。 */

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    /** @param redisKeyPrefix Redis 水位命名空间。 */

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
