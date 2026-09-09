package com.peach.common.key;

import com.peach.common.util.StringUtil;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * 通用 Key 构建器。
 * <p>
 * 封装 key 原文和模板格式化逻辑，供多个模块统一生成最终 key；具体业务 key 定义由所属模块维护。
 * </p>
 */
public final class KeyBuilder {

    private final String realKey;

    private KeyBuilder(String realKey) {
        if (StringUtil.isBlank(realKey)) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.realKey = realKey;
    }

    /**
     * 使用完整 key 字符串创建构建结果。
     *
     * @param key 完整 key 字符串
     * @return key 构建结果
     * @throws IllegalArgumentException key 为空白时抛出
     */
    public static KeyBuilder of(String key) {
        return new KeyBuilder(key);
    }

    /**
     * 使用 {@link MessageFormat} 模板创建构建结果。
     *
     * @param pattern key 模板
     * @param args 模板参数
     * @return key 构建结果
     * @throws IllegalArgumentException pattern 为空白或格式化结果为空白时抛出
     */
    public static KeyBuilder format(String pattern, Object... args) {
        if (StringUtil.isBlank(pattern)) {
            throw new IllegalArgumentException("pattern must not be blank");
        }
        return new KeyBuilder(MessageFormat.format(pattern, args));
    }

    /**
     * 使用 key 模板契约创建构建结果。
     *
     * @param template key 模板契约
     * @param args 模板参数
     * @return key 构建结果
     * @throws NullPointerException template 为 null 时抛出
     * @throws IllegalArgumentException template pattern 为空白或格式化结果为空白时抛出
     */
    public static KeyBuilder from(KeyTemplate template, Object... args) {
        Objects.requireNonNull(template, "template must not be null");
        return format(template.pattern(), args);
    }

    /**
     * 获取最终生成的 key。
     *
     * @return 最终 key 字符串
     */
    public String getRealKey() {
        return realKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyBuilder that)) {
            return false;
        }
        return realKey.equals(that.realKey);
    }

    @Override
    public int hashCode() {
        return realKey.hashCode();
    }

    @Override
    public String toString() {
        return realKey;
    }
}
