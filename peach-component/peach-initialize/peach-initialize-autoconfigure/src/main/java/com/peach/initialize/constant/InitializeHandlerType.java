package com.peach.initialize.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:57
 */
public final class InitializeHandlerType {

    private InitializeHandlerType() {
        throw new IllegalStateException("Utility class");
    }

    public static final String APP_INITIALIZING_BEAN = "APP_INITIALIZING_BEAN";

    public static final String APP_COMMAND_LINE_RUNNER = "APP_COMMAND_LINE_RUNNER";

    public static final String APP_POSTCNSTRUCT = "APP_POSTCONSTRUCT";

    public static final String APP_EVENT_LISTENER = "APP_EVENT_LISTENER";
}
