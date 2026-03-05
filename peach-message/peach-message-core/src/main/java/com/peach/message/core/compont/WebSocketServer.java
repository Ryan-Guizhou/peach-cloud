package com.peach.message.core.compont;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peach.message.core.context.WebSocketContext;
import com.peach.message.core.context.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:09
 */
@Slf4j
@Indexed
@Component
@ServerEndpoint(value = "/webSocket/msg/{type}")
public class WebSocketServer {

    public static final String WEBSOCKET_TOPIC = "WEBSOCKET_MSG_TOPIC";
    private static final int MAX_MESSAGES_PER_SECOND = 60;
    private static final ConcurrentHashMap<String, MsgRateCounter> MSG_RATE = new ConcurrentHashMap<>();

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
            this.type = StringUtils.defaultIfBlank(type, "default");
            if (WebSocketContext.getContext() != null && StringUtils.isNotBlank(WebSocketContext.getContext().getUserId())) {
                this.sid = WebSocketContext.getContext().getUserId();
            } else {
                this.sid = session.getId();
            }

            this.session.getUserProperties().put("userId", this.sid);
            this.session.getUserProperties().put("type", this.type);
            this.session.setMaxIdleTimeout(60_000L);

            TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(this.type);
            if (webSocketSet == null) {
                webSocketSet = new TimedCache<>(12 * 1000 * 60 * 60);
            } else {
                webSocketSet.remove(session.getId());
            }
            webSocketSet.put(session.getId(), this);
            Context.CACHE.put(this.type, webSocketSet);

            this.session.getAsyncRemote().sendText("{\"message\":\"connected!\"}");
            log.info("WebSocket connected, sid={}, type={}, sessionId={}", sid, this.type, session.getId());
        } catch (Exception e) {
            log.error("WebSocket onOpen failed, type={}, sessionId={}", type, session == null ? "null" : session.getId(), e);
        } finally {
            WebSocketContext.removeContext();
        }
    }

    @OnClose
    public void onClose(Session session) {
        WebSocketContext.removeContext();
        MSG_RATE.remove(session.getId());
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            webSocketSet.remove(session.getId());
            log.info("WebSocket closed, sid={}, type={}, sessionId={}", sid, type, session.getId());
        }
        try {
            session.close();
        } catch (IOException e) {
            log.error("WebSocket close session failed, sid={}, type={}, sessionId={}", sid, type, session.getId(), e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (isRateLimited(session.getId())) {
            log.warn("WebSocket message dropped by rate limit, sid={}, type={}, sessionId={}", sid, type, session.getId());
            return;
        }
        log.info("WebSocket message received, sid={}, type={}, sessionId={}, size={}", sid, type, session.getId(),
                message == null ? 0 : message.length());
        session.getAsyncRemote().sendText(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        WebSocketContext.removeContext();
        if (session != null) {
            MSG_RATE.remove(session.getId());
        }
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(this.type);
        if (webSocketSet != null && session != null) {
            webSocketSet.remove(session.getId());
        }
        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            log.error("WebSocket close onError failed, sid={}, type={}, sessionId={}", sid, type, session == null ? "null" : session.getId(), e);
        }
        log.error("WebSocket onError, sid={}, type={}, sessionId={}", sid, type, session == null ? "null" : session.getId(), error);
    }

    private void publishMessage(WebSocketMessage message) {
        try {
            RedisTemplate<String, Object> redisTemplate = SpringUtil.getBean("redisTemplate");
            if (redisTemplate != null) {
                redisTemplate.convertAndSend(WEBSOCKET_TOPIC, JSON.toJSONString(message));
            } else {
                log.error("RedisTemplate not found");
            }
        } catch (Exception e) {
            log.error("Publish message error, type={}, sid={}", message == null ? "null" : message.getType(), message == null ? "null" : message.getSid(), e);
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
        if (jsonMap != null) {
            jsonMap.forEach((sid, json) -> sendMessage(json.toJSONString(), type, sid));
        }
    }

    public void sendLocalMessage(String message, String type, String sid) {
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            for (WebSocketServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid) && item.isOpen()) {
                        item.session.getAsyncRemote().sendText(message);
                    }
                } catch (Exception e) {
                    log.error("send message error, type={}, sid={}", type, sid, e);
                }
            }
        }
    }

    public void sendLocalMessage(String message, String type) {
        TimedCache<String, WebSocketServer> webSocketSet = Context.CACHE.get(type);
        if (webSocketSet != null) {
            for (WebSocketServer item : webSocketSet) {
                try {
                    if (item.isOpen()) {
                        item.session.getAsyncRemote().sendText(message);
                    }
                } catch (Exception e) {
                    log.error("send message error, type={}", type, e);
                }
            }
        }
    }

    public void sendLocalMessage(String message) {
        Context.CACHE.forEach((k, socketServers) -> {
            if (socketServers != null) {
                for (WebSocketServer item : socketServers) {
                    try {
                        if (item.isOpen()) {
                            item.session.getAsyncRemote().sendText(message);
                        }
                    } catch (Exception e) {
                        log.error("send broadcast all message error", e);
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
                    log.error("close session error, type={}, sid={}", type, sid, e);
                }
            }
        }
    }

    public String getSessionId() {
        return session == null ? null : session.getId();
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    private boolean isRateLimited(String sessionId) {
        long second = System.currentTimeMillis() / 1000L;
        MsgRateCounter counter = MSG_RATE.computeIfAbsent(sessionId, k -> new MsgRateCounter(second));
        synchronized (counter) {
            if (counter.windowSecond != second) {
                counter.windowSecond = second;
                counter.count.set(0);
            }
            return counter.count.incrementAndGet() > MAX_MESSAGES_PER_SECOND;
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

    private static final class MsgRateCounter {
        private long windowSecond;
        private final AtomicInteger count = new AtomicInteger(0);

        private MsgRateCounter(long windowSecond) {
            this.windowSecond = windowSecond;
        }
    }
}
