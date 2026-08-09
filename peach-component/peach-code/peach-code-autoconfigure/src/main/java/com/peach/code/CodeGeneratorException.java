package com.peach.code;

/**
 * 业务编码生成失败异常。
 *
 * <p>用于表示编码规则缺失、配置非法、规则停用、调用事务边界错误或序号达到上限等
 * 可识别的业务失败，不用于表示 Redis 水位更新失败。</p>
 */
public class CodeGeneratorException extends RuntimeException {

    /**
     * 使用指定消息创建异常。
     *
     * @param message 失败原因
     */
    public CodeGeneratorException(String message) {
        super(message);
    }

    /**
     * 使用指定消息和原始异常创建异常。
     *
     * @param message 失败原因
     * @param cause 原始异常
     */
    public CodeGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
