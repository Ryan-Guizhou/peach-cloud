package com.peach.observability.core;

import java.util.UUID;

/**
 * 基于随机 UUID 的默认请求 ID 生成器。
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
