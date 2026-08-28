package com.peach.message.service;

import com.peach.message.dto.WebSocketMessageDTO;

/**
 * WebSocket 推送服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket 推送服务接口
 */
public interface IWebSocketPushService {

    /**
     * 发布推送消息
     *
     * @param message 推送消息
     */
    void publish(WebSocketMessageDTO message);

    /**
     * 推送给指定用户
     *
     * @param userId 用户ID
     * @param type 事件类型
     * @param payload 消息体
     */
    void pushToUser(String userId, String type, Object payload);

    /**
     * 广播给全部在线用户
     *
     * @param type 事件类型
     * @param payload 消息体
     */
    void broadcast(String type, Object payload);
}
