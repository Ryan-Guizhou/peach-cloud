package com.peach.message.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息模块常量
 */
public interface MessageConst {

    String MODULE_CODE = "MESSAGE";

    String WEBSOCKET_ENDPOINT = "/webSocket/message";

    String WEBSOCKET_REDIS_TOPIC = "peach:message:websocket:push";

    String DEFAULT_WEBSOCKET_CHANNEL = "message";
}
