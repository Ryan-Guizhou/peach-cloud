package com.peach.code;

/**
 * 租户业务编码生成器。
 *
 * <p>默认实现仅使用 MySQL 事务行锁分配序号。同一租户和编码前缀只能对应一条规则；
 * 外部业务应使用唯一索引保护最终写入。发号事务独立提交，不随调用方业务事务回滚。</p>
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
     * @param prefix 编码前缀，只允许字母、数字和连字符
     * @return 业务编码，例如 {@code MENU_00000001}
     * @throws CodeGeneratorException 参数非法、规则缺失或停用、序号溢出、数据库分配失败时抛出
     */
    String next(String tenantId, String prefix);
}
