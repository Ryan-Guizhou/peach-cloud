package com.peach.common.key;

/**
 * Key 定义维护契约。
 * <p>
 * 用于业务或组件模块维护可复用 key 的模板和说明信息。peach-common 只提供公共契约，
 * 不保存验证码、认证、消息等具体业务 key。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public interface KeyDefinition extends KeyTemplate {

    /**
     * 获取 key 所属模块编码。
     *
     * @return 模块编码，例如 COMMON、CAPTCHA、AUTH
     */
    String moduleCode();

    /**
     * 获取 key 的用途说明。
     *
     * @return key 用途说明
     */
    String keyIntroduce();

    /**
     * 获取 value 的用途说明。
     *
     * @return value 用途说明
     */
    String valueIntroduce();

    /**
     * 获取 key 定义的维护人标识。
     *
     * @return 维护人标识
     */
    String author();
}
