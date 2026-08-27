package com.peach.message.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;

import com.alibaba.fastjson.JSON;
import com.peach.common.IDGeneratorUtil;
import com.peach.message.common.MessageConst;
import com.peach.message.common.enums.MessageEnum;
import com.peach.message.dto.WebSocketMessageDTO;
import com.peach.message.service.IWebSocketPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket 推送服务实现
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class WebSocketPushServiceImpl implements IWebSocketPushService {

        private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publish(WebSocketMessageDTO message) {
        if (message == null) {
            return;
        }
        if (message.getTraceId() == null) {
            message.setTraceId(IDGeneratorUtil.generateUuid());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        redisTemplate.convertAndSend(MessageConst.WEBSOCKET_REDIS_TOPIC, JSON.toJSONString(message));
    }

    @Override
    public void pushToUser(String userId, String type, Object payload) {
        WebSocketMessageDTO message = new WebSocketMessageDTO();
        message.setMode(MessageEnum.WsPushMode.SINGLE);
        message.setType(type);
        message.setUserIds(List.of(userId));
        message.setPayload(payload);
        publish(message);
    }

    @Override
    public void broadcast(String type, Object payload) {
        WebSocketMessageDTO message = new WebSocketMessageDTO();
        message.setMode(MessageEnum.WsPushMode.BROADCAST);
        message.setType(type);
        message.setPayload(payload);
        publish(message);
    }
}
