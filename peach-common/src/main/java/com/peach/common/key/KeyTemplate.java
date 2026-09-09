package com.peach.common.key;

/**
 * Key 模板契约。
 * <p>
 * 只描述可格式化的 key pattern，不维护具体业务或组件的 key 定义。业务模块需要沉淀 key 时，
 * 应在所属模块实现该接口或 {@link KeyDefinition}。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public interface KeyTemplate {

    /**
     * 获取可被 {@link java.text.MessageFormat} 格式化的 key 模板。
     *
     * @return key 模板
     */
    String pattern();
}
