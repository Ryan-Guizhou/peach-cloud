package com.peach.common.unique;

/**
 * ID 生成器全局常量与配置项定义。
 *
 * <p>包含支持的 ID 算法标识名称、JVM 启动参数键名、环境变量配置键名以及多级回退的默认值。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public final class UniqueGeneratorConst {

    private UniqueGeneratorConst() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 32 位无短横线 UUID 算法标识（例如：{@code c0a80164e8b84d9f95712e411234abcd}）。
     */
    public static final String UUID = "uuid";

    /**
     * 21 位紧凑型 URL 安全 NanoID 算法标识（例如：{@code V1StGXR8_Z5jdHi6B-myT}）。
     */
    public static final String NANOID = "nanoid";

    /**
     * 64 位有序递增雪花算法（Snowflake）标识（例如：{@code 1834273829103829102}）。
     */
    public static final String SNOWFLAKE = "snowflake";

    /**
     * 全局默认 ID 生成算法的 JVM 系统属性配置键。
     * <p>例如：{@code -Dpeach.common.id.type=nanoid}</p>
     */
    public static final String PROPERTY_DEFAULT_TYPE = "peach.common.id.type";

    /**
     * 全局默认 ID 生成算法的操作系统环境变量配置键。
     * <p>例如：{@code PEACH_COMMON_ID_TYPE=snowflake}</p>
     */
    public static final String ENV_DEFAULT_TYPE = "PEACH_COMMON_ID_TYPE";

    /**
     * 雪花算法工作机器 ID（worker-id，取值范围 0~31）的 JVM 系统属性配置键。
     * <p>例如：{@code -Dpeach.common.id.snowflake.worker-id=2}</p>
     */
    public static final String PROPERTY_SNOWFLAKE_WORKER_ID = "peach.common.id.snowflake.worker-id";

    /**
     * 雪花算法工作机器 ID（worker-id）的标准环境变量配置键。
     * <p>例如：{@code PEACH_COMMON_ID_SNOWFLAKE_WORKER_ID=2}</p>
     */
    public static final String ENV_SNOWFLAKE_WORKER_ID = "PEACH_COMMON_ID_SNOWFLAKE_WORKER_ID";

    /**
     * 雪花算法工作机器 ID（worker-id）的简写环境变量配置键（兼容别名）。
     * <p>例如：{@code PEACH_ID_WORKER_ID=2}</p>
     */
    public static final String ENV_SNOWFLAKE_WORKER_ID_SHORT = "PEACH_ID_WORKER_ID";

    /**
     * 雪花算法数据中心 ID（datacenter-id，取值范围 0~31）的 JVM 系统属性配置键。
     * <p>例如：{@code -Dpeach.common.id.snowflake.datacenter-id=1}</p>
     */
    public static final String PROPERTY_SNOWFLAKE_DATACENTER_ID = "peach.common.id.snowflake.datacenter-id";

    /**
     * 雪花算法数据中心 ID（datacenter-id）的标准环境变量配置键。
     * <p>例如：{@code PEACH_COMMON_ID_SNOWFLAKE_DATACENTER_ID=1}</p>
     */
    public static final String ENV_SNOWFLAKE_DATACENTER_ID = "PEACH_COMMON_ID_SNOWFLAKE_DATACENTER_ID";

    /**
     * 雪花算法数据中心 ID（datacenter-id）的简写环境变量配置键（兼容别名）。
     * <p>例如：{@code PEACH_ID_DATACENTER_ID=1}</p>
     */
    public static final String ENV_SNOWFLAKE_DATACENTER_ID_SHORT = "PEACH_ID_DATACENTER_ID";

    /**
     * 缺省默认 ID 算法：UUID（完全保持原有 32 位无短横线行为兼容）。
     */
    public static final String DEFAULT_TYPE = UUID;

    /**
     * 雪花算法机器 ID 最终兜底默认值。
     */
    public static final long DEFAULT_WORKER_ID = 1L;

    /**
     * 雪花算法数据中心 ID 最终兜底默认值。
     */
    public static final long DEFAULT_DATACENTER_ID = 1L;
}
