package com.peach.message.common;

/**
 * 消息模块常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息模块常量
 */
public final class MessageConst {

    private MessageConst() {
        throw new IllegalStateException("Utility class");
    }

    public static final String MODULE_CODE = "MESSAGE";

    public static final String WEBSOCKET_ENDPOINT = "/webSocket/message";

    public static final String WEBSOCKET_REDIS_TOPIC = "peach:message:websocket:push";

    public static final String DEFAULT_WEBSOCKET_CHANNEL = "message";
}
