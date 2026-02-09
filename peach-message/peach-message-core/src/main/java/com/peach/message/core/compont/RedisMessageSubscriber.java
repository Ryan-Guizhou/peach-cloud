package com.peach.message.core.compont;

import com.alibaba.fastjson.JSON;
import com.peach.message.core.context.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisMessageSubscriber implements MessageListener {


    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String msgBody = new String(message.getBody());
             log.info("Received Redis Message: " + msgBody);
            WebSocketMessage webSocketMessage = JSON.parseObject(msgBody, WebSocketMessage.class);
            
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
}
