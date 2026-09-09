package com.peach.common.unique;

/**
 * ID 生成器 SPI（Service Provider Interface）扩展提供者接口。
 *
 * <p>第三方或自定义模块可通过实现此接口并在 {@code META-INF/services/com.peach.common.unique.IdGeneratorProvider}
 * 中声明实现类，实现对自定义 ID 生成算法的无侵入式接入。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public interface UniqueGeneratorProvider {

    /**
     * 该提供者所支持的算法类型唯一标识。
     *
     * @return 算法标识字符串（大小写不敏感），例如 "uuid", "nanoid", "snowflake"
     */
    String type();

    /**
     * 获取该提供者对应的 {@link UniqueGenerator} 实例。
     *
     * @return ID 生成器实例
     */
    UniqueGenerator getGenerator();
}
