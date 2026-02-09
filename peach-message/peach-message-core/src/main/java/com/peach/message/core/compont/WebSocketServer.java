package com.peach.message.core.compont;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peach.message.core.context.WebSocketContext;
import com.peach.message.core.context.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ServerEndpoint(value = "/webSocket/msg/{type}")
@Component
public class WebSocketServer {
    
    public static final String WEBSOCKET_TOPIC = "WEBSOCKET_MSG_TOPIC";

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收sid 请求唯一标识
     */
    private String sid = "";
    /**
     * 接收类型
     */
    private String type = "";

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "type") String type) {
        try {
            this.session = session;
            this.sid = WebSocketContext.getContext().getUserId();
            this.type = type;
            TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
            if (webSocketSet == null) {
                webSocketSet = new TimedCache<>(12 * 1000 * 60 * 60);
            } else {
                webSocketSet.remove(session.getId());
            }
            webSocketSet.put(session.getId(), this);
            Context.CACHE.put(type, webSocketSet);
            this.session.getAsyncRemote().sendText("{\"message\":\"connected!\"}");
            log.error(this + " connected: " + sid + " " + type);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            WebSocketContext.removeContext();
        }
    }

    @OnClose
    public void onClose(Session session) {
        WebSocketContext.removeContext();
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            webSocketSet.remove(session.getId());
            log.error(this + ",closed: " + session.getId() + " " + type);
        }
        try {
            session.close();
        } catch (IOException e) {
            log.error(this + " onClose error: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info(this + ",message: " + message);
        // 收到客户端消息，目前是直接回显，也可以扩展为处理业务
        session.getAsyncRemote().sendText(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        WebSocketContext.removeContext();
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(this.type);
        if (webSocketSet != null) {
            webSocketSet.remove(session.getId());
        }
        try {
            session.close();
        } catch (IOException e) {
            log.error("onError error: " + e.getMessage());
        }
        log.info(this + " onError: " + error.getMessage());
    }

    // ================== 分布式发送逻辑 (Publish) ==================

    /**
     * 发布消息到 Redis
     */
    private void publishMessage(WebSocketMessage message) {
        try {
            // 获取 RedisTemplate (懒加载，因为 WebSocketServer 实例可能不是由 Spring 创建)
            RedisTemplate<String, Object> redisTemplate = SpringUtil.getBean("redisTemplate");
            if (redisTemplate != null) {
                redisTemplate.convertAndSend(WEBSOCKET_TOPIC, JSON.toJSONString(message));
            } else {
                log.error("RedisTemplate not found!");
            }
        } catch (Exception e) {
            log.error("Publish message error: " + e.getMessage());
        }
    }

    public void sendMessage(String message, String type) {
        publishMessage(WebSocketMessage.broadcastType(type, message));
    }

    public void sendMessage(String message) {
        publishMessage(WebSocketMessage.broadcastAll(message));
    }

    public void sendMessage(String message, String type, String sid) {
        publishMessage(WebSocketMessage.unicast(type, sid, message));
    }
    
    public void closeSession(String type, String sid) {
        publishMessage(WebSocketMessage.close(type, sid));
    }
    
    public void sendMessage(Map<String, JSONObject> jsonMap, String type) {
         // 这种批量发送比较特殊，jsonMap key是sid，value是消息
         // 为了简单，我们这里可以循环发送单播，或者定义一个新的批量消息类型
         // 这里简化处理：循环发送单播 (虽然效率略低，但兼容性好)
         if (jsonMap != null) {
             jsonMap.forEach((sid, json) -> {
                 sendMessage(json.toJSONString(), type, sid);
             });
         }
    }

    // ================== 本地发送逻辑 (由 Redis Subscriber 调用) ==================

    public void sendLocalMessage(String message, String type, String sid) {
        log.info("Local send unicast: sid=" + sid + ", type=" + type);
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            for (WebSocketServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid)) {
                        item.session.getAsyncRemote().sendText(message);
                    }
                } catch (Exception e) {
                    log.error("send message error:" + e);
                }
            }
        }
    }

    public void sendLocalMessage(String message, String type) {
        log.info("Local send broadcast type: type=" + type);
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            for (WebSocketServer item : webSocketSet) {
                try {
                    item.session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    log.error("send message error:" + e);
                }
            }
        }
    }

    public void sendLocalMessage(String message) {
        log.info("Local send broadcast all");
        Context.CACHE.forEach((k, socketServers) -> {
            if (socketServers != null) {
                for (WebSocketServer item : socketServers) {
                    try {
                        item.session.getAsyncRemote().sendText(message);
                    } catch (Exception e) {
                        log.error("send message error:" + e);
                    }
                }
            }
        });
    }

    public void closeLocalSession(String type, String sid) {
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            for (WebSocketServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid)) {
                        item.session.close();
                        webSocketSet.remove(item.session.getId());
                        break;
                    }
                } catch (Exception e) {
                    log.error("close error: " + e);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketServer that = (WebSocketServer) o;
        return Objects.equals(session, that.session) && Objects.equals(sid, that.sid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, sid);
    }
}
