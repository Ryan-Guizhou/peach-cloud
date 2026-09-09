package com.peach.common.unique;

/**
 * 全局统一 ID 生成工具类（门面），支持基于 SPI 扩展的多种 ID 生成算法。
 *
 * <p>开箱即用支持以下算法：
 * <ul>
 *   <li>{@link UniqueGeneratorConst#UUID}：32 位无短横线 UUID（默认兼顾存量兼容，格式如：{@code c0a80164e8b84d9f95712e411234abcd}）。</li>
 *   <li>{@link UniqueGeneratorConst#NANOID}：21 位紧凑 URL 安全 ID（格式如：{@code V1StGXR8_Z5jdHi6B-myT}）。</li>
 *   <li>{@link UniqueGeneratorConst#SNOWFLAKE}：64 位有序递增雪花 ID（格式如：{@code 1834273829103829102}，支持转 long）。</li>
 * </ul>
 * </p>
 *
 * <p>启动参数与环境变量配置：
 * <ul>
 *   <li>默认算法切换：系统属性 {@code -Dpeach.common.id.type=nanoid} 或环境变量 {@code PEACH_COMMON_ID_TYPE=nanoid}。</li>
 *   <li>雪花算法机器 ID：系统属性 {@code -Dpeach.common.id.snowflake.worker-id=2} 或环境变量 {@code PEACH_COMMON_ID_SNOWFLAKE_WORKER_ID}。</li>
 *   <li>雪花算法数据中心 ID：系统属性 {@code -Dpeach.common.id.snowflake.datacenter-id=1} 或环境变量 {@code PEACH_COMMON_ID_SNOWFLAKE_DATACENTER_ID}。</li>
 *   <li>多级兜底策略：未显式配置时，默认算法自动回退至 {@code uuid}；雪花算法自动根据本机 IP 与 MAC 特征哈希推导，完全零配置即可安全运行。</li>
 * </ul>
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.1.0
 * @CreateTime 2024/10/10 15:22
 */
public final class UniqueIdFacade {


    private UniqueIdFacade() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 使用当前配置的全局默认生成器生成 ID。
     *
     * <p>可通过系统属性 {@code -Dpeach.common.id.type=nanoid} 或环境变量 {@code PEACH_COMMON_ID_TYPE} 切换默认策略，缺省为 UUID。</p>
     *
     * @return 唯一字符串 ID
     */
    public static String nextId() {
        return UniqueGeneratorRegistry.getDefaultGenerator().nextId();
    }

    /**
     * 使用指定类型的算法生成 ID。
     *
     * @param type 算法标识，见 {@link UniqueGeneratorConst}，如 {@link UniqueGeneratorConst#NANOID}
     * @return 唯一字符串 ID
     * @throws IllegalArgumentException 当传入的算法类型未注册时抛出
     */
    public static String nextId(String type) {
        return UniqueGeneratorRegistry.getGenerator(type).nextId();
    }

    /**
     * 使用雪花算法生成 64 位长整型正整数 ID。
     *
     * @return 64 位数值型唯一 ID
     */
    public static long nextLongId() {
        return UniqueGeneratorRegistry.getGenerator(UniqueGeneratorConst.SNOWFLAKE).nextLongId();
    }

    /**
     * 生成 32 位无短横线 UUID。
     *
     * <p>保留存量方法以保证 100% 接口与二进制向后兼容，内部直接委托至 UUID 生成策略。</p>
     *
     * @return 32 位十六进制 UUID 字符串（不带横杠）
     */
    public static String generateUuid() {
        return nextId(UniqueGeneratorConst.UUID);
    }

    /**
     * 快捷生成 21 位 URL 安全 NanoID。
     *
     * @return 21 位 NanoID 字符串
     */
    public static String generateNanoId() {
        return nextId(UniqueGeneratorConst.NANOID);
    }

    /**
     * 快捷生成雪花算法字符串 ID。
     *
     * @return 雪花算法数字字符串
     */
    public static String generateSnowflakeId() {
        return nextId(UniqueGeneratorConst.SNOWFLAKE);
    }

    /**
     * 获取指定算法的生成器实例以执行高级定制。
     *
     * @param type 算法标识，见 {@link UniqueGeneratorConst}
     * @return 生成器实例
     * @throws IllegalArgumentException 当传入的算法类型未注册时抛出
     */
    public static UniqueGenerator getGenerator(String type) {
        return UniqueGeneratorRegistry.getGenerator(type);
    }

}
