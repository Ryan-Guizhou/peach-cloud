package com.peach.message.core.compont;

import com.alibaba.fastjson.JSON;
import com.peach.message.core.context.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.nio.charset.StandardCharsets;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:09
 */
@Slf4j
@Indexed
@Component
public class RedisMessageSubscriber implements MessageListener {


    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String msgBody = new String(message.getBody(), StandardCharsets.UTF_8);
             log.info("Received Redis Message: " + msgBody);
            WebSocketMessage webSocketMessage = parseMessage(msgBody);
            
            if (webSocketMessage == null) return;

            switch (webSocketMessage.getMsgType()) {
                case UNICAST:
                    webSocketServer.sendLocalMessage(webSocketMessage.getContent(), webSocketMessage.getType(), webSocketMessage.getSid());
                    break;
                case BROADCAST_TYPE:
                    webSocketServer.sendLocalMessage(webSocketMessage.getContent(), webSocketMessage.getType());
                    break;
                case BROADCAST_ALL:
                    webSocketServer.sendLocalMessage(webSocketMessage.getContent());
                    break;
                case CLOSE:
                    webSocketServer.closeLocalSession(webSocketMessage.getType(), webSocketMessage.getSid());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling Redis message", e);
        }
    }

    public WebSocketMessage parseMessage(String jsonStr) {
        try {
            // 使用正则表达式移除非打印字符
            String cleanStr = jsonStr.replaceAll("[\\x00-\\x1F\\x7F]", "");

            // 处理可能的双重JSON字符串
            if (cleanStr.startsWith("\"") && cleanStr.endsWith("\"")) {
                // 如果是双重JSON，先解析外层
                String innerJson = JSON.parseObject(cleanStr, String.class);
                return JSON.parseObject(innerJson, WebSocketMessage.class);
            } else {
                return JSON.parseObject(cleanStr, WebSocketMessage.class);
            }
        } catch (Exception e) {
            log.error("Error parsing Redis message", e);
            return null;
        }
    }
}
