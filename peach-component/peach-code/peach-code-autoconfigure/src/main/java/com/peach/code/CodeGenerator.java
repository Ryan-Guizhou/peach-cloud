package com.peach.code;

/**
 * 租户业务编码生成器。
 *
 * <p>默认实现使用 MySQL 行锁、持久账本与可选 Redis 水位同步。同一租户和稳定业务类型
 * 只能对应一条规则；前缀、数字宽度可以直接修改规则表，只影响未来编码。
 * 发号事务独立提交，不随调用方业务事务回滚。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public interface CodeGenerator {

    /**
     * 分配下一个业务编码。
     *
     * @param tenantId 租户标识，只允许字母、数字和连字符
     * @param codeType 稳定业务类型，例如 MENU、ROLE、RESOURCE、FUNCTION；忽略大小写
     * @return 业务编码，例如 {@code M00001}，不附加分隔符
     * @throws CodeGeneratorException 参数非法、规则缺失或停用、序号溢出、数据库分配失败时抛出
     */
    String next(String tenantId, String codeType);
}
