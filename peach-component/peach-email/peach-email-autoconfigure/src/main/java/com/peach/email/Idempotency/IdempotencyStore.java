package com.peach.email.Idempotency;

import com.peach.email.core.SendResult;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:18
 * @Description: 幂等存储接口
 */
public interface IdempotencyStore {

    /**
     * 检查是否存在重复请求
     * @param key 幂等键
     * @return 是否存在重复请求
     */
    boolean exists(String key);

    /**
     * 保存幂等键
     * @param key 幂等键
     */
    void record(String key, SendResult result);
}
