package com.peach.code;

/**
 * 租户业务编码生成器。
 *
 * <p>编码规则由 {@code PEACH_CODE_RULE} 表维护。默认实现采用 Redis 优先、MySQL 兜底策略；
 * 业务方应通过业务表唯一索引保证最终不重复，并根据业务需要将生成和业务写入放在同一事务中。</p>
 */
public interface CodeGenerator {

    /**
     * 为指定租户和前缀分配下一个业务编码。
     *
     * <p>默认实现不要求当前存在事务。Redis 主路径分配的序号不会随业务事务回滚而回收；
     * Redis 不可用时，MySQL 兜底使用独立事务提交序号。</p>
     *
     * @param tenantId 租户标识，只允许字母、数字和连字符
     * @param prefix 编码前缀，只允许字母、数字和连字符
     * @return 按规则格式化后的业务编码，例如 {@code MENU_00000001}
     * @throws CodeGeneratorException 租户或前缀非法、规则不存在、规则停用、序号溢出或存储不可用时抛出
     */
    String next(String tenantId, String prefix);
}
