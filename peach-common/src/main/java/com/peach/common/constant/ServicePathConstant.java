package com.peach.common.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 17:21
 * @Description 服务间调用路径常量
 */
public final class ServicePathConstant {

    private ServicePathConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String PATH_PREFIX = "/";

    private static final String EXTERNAL = "external";

    public static final String AUTH_PATH_SERVICE = PATH_PREFIX + "auth" + PATH_PREFIX + EXTERNAL;

    public static final String MONITOR_PATH_SERVICE = PATH_PREFIX + "monitor" + PATH_PREFIX + EXTERNAL;

    public static final String FILE_PATH_SERVICE = PATH_PREFIX + "fileservice" + PATH_PREFIX + EXTERNAL;

    public static final String MESSAGE_PATH_SERVICE = PATH_PREFIX + "message" + PATH_PREFIX + EXTERNAL;

    public static final String SETTING_PATH_SERVICE = PATH_PREFIX + "setting" + PATH_PREFIX + EXTERNAL;

    public static final String SCHEDULED_PATH_SERVICE = PATH_PREFIX + "scheduled" + PATH_PREFIX + EXTERNAL;

    public static final String GENERATOR_PATH_SERVICE = PATH_PREFIX + "generator" + PATH_PREFIX + EXTERNAL;
}
