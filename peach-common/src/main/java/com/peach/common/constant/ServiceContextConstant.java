package com.peach.common.constant;

/**
 * 业务服务的fegin上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 16:52
 * @Description 业务服务的fegin上下文
 */
public final class ServiceContextConstant {

    private ServiceContextConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String AUTH_SERVICE_CONTEXT = "authFeignClient";

    public static final String MONITOR_SERVICE_CONTEXT = "monitorFeignClient";

    public static final String FILE_SERVICE_CONTEXT = "fileFeignClient";

    public static final String MESSAGE_SERVICE_CONTEXT = "messageFeignClient";

    public static final String SETTING_SERVICE_CONTEXT = "settingFeginClient";

    public static final String GENERATOR_SERVICE_CONTEXT = "generatorFeginClient";

    public static final String SCHEDULED_SERVICE_CONTEXT = "scheduledFeginClient";

}
