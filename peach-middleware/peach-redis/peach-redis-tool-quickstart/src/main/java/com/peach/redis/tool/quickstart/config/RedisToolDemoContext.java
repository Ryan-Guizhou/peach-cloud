package com.peach.redis.tool.quickstart.config;

import com.peach.common.key.KeyBuilder;
import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 单次示例的 Key 清单；只登记精确数据键，通道和匹配模板不参与自动清理。
 * 生命周期由 Spring 管理；同步演示使用，运行期间不得更换运行标识。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
@Component
public class RedisToolDemoContext {
    private final String runId;
    private final RedisDao redisDao;
    private final Set<String> keys = new LinkedHashSet<>();

    public RedisToolDemoContext(RedisToolDemoProperties properties, RedisDao redisDao) {
        this.runId = properties.getRunId();
        if (runId == null || !runId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Demo run-id must contain 1-64 letters, digits, underscores or hyphens");
        }
        this.redisDao = redisDao;
    }

    /** 构建并登记精确键；业务标识限制为非敏感的简单标识符。 */
    public synchronized String key(RedisToolKey definition, String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Demo id must be a simple non-sensitive identifier");
        }
        if (definition == RedisToolKey.CHANNEL || definition == RedisToolKey.DELETE_PATTERN) {
            throw new IllegalArgumentException("Channels and patterns are not data keys");
        }
        String key = KeyBuilder.from(definition, runId, id).getRealKey();
        keys.add(key);
        return key;
    }

    public String deletePattern() {
        return KeyBuilder.from(RedisToolKey.DELETE_PATTERN, runId).getRealKey();
    }

    public String channel() {
        return KeyBuilder.from(RedisToolKey.CHANNEL, runId, "events").getRealKey();
    }

    /** 精确清理全部登记键；失败时仍尝试剩余键，最后抛出首个异常。 */
    public synchronized void clear() {
        RuntimeException failure = null;
        for (String key : keys) {
            try {
                redisDao.delete(key);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
