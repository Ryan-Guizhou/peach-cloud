package com.peach.email.idempotency;

import com.peach.email.core.SendResult;

/**
 * Idempotency存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:18
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
    void storeSendResult(String key, SendResult result);
}
