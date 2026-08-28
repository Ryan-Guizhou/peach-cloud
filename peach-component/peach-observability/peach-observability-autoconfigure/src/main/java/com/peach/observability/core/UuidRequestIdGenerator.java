package com.peach.observability.core;

import java.util.UUID;

/**
 * Uuid请求Id生成器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class UuidRequestIdGenerator implements RequestIdGenerator {

    /**
     * 生成移除连字符的随机 UUID。
     *
     * @return 长度为 32 的小写十六进制请求 ID
     */
    @Override
    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
