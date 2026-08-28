package com.peach.message.config;

import lombok.RequiredArgsConstructor;

import com.peach.message.common.MessageConst;
import com.peach.message.websocket.MessageWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * WebSocket 配置类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket 配置类
 */
@Indexed
@Configuration
@RequiredArgsConstructor
public class WebSocketConfiguration {

        private final MessageWebSocketHandler messageWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(MessageConst.WEBSOCKET_ENDPOINT, messageWebSocketHandler));
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
