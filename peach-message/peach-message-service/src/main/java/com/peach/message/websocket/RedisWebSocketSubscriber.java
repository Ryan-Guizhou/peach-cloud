package com.peach.message.websocket;

import lombok.RequiredArgsConstructor;

import com.alibaba.fastjson.JSON;
import com.peach.message.common.enums.MessageEnum;
import com.peach.message.dto.WebSocketMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebSocket Redis订阅处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket Redis订阅处理器
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class RedisWebSocketSubscriber implements MessageListener {

        private final WebSocketSessionManager sessionManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        WebSocketMessageDTO pushMessage;
        try {
            pushMessage = JSON.parseObject(message.getBody(), WebSocketMessageDTO.class);
        } catch (Exception e) {
            log.error("Deserialize WebSocket push message failed, body={}",
                    new String(message.getBody(), StandardCharsets.UTF_8), e);
            return;
        }
        if (pushMessage == null) {
            return;
        }
        String jsonMsg = JSON.toJSONString(pushMessage);
        MessageEnum.WsPushMode mode = pushMessage.getMode() == null
                ? MessageEnum.WsPushMode.SINGLE : pushMessage.getMode();
        if (MessageEnum.WsPushMode.BROADCAST.equals(mode)) {
            sessionManager.broadcastAll(jsonMsg);
            return;
        }
        List<String> userIds = pushMessage.getUserIds();
        if (userIds != null && !userIds.isEmpty()) {
            for (String userId : userIds) {
                sessionManager.pushToUser(userId, jsonMsg);
            }
        }
        List<String> channels = pushMessage.getChannels();
        if (channels != null && !channels.isEmpty()) {
            for (String channel : channels) {
                sessionManager.pushToChannel(channel, jsonMsg);
            }
        }
    }
}
