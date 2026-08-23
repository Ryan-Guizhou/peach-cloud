package com.peach.observability.core;

/**
 * 请求 ID 生成器。
 *
 * <p>业务方可以声明同类型 Bean 覆盖默认实现。生成值必须适合写入 HTTP Header 和日志上下文，
 * 且不得携带用户信息、租户信息或其他敏感数据。</p>
 */
@FunctionalInterface
public interface RequestIdGenerator {

    /**
     * 生成新的请求 ID。
     *
     * @return 非空且符合当前请求 ID 校验规则的标识
     */
    String generate();
}
