package com.peach.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 本实例连接管理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket 本实例连接管理器
 */
@Slf4j
@Indexed
@Component
public class WebSocketSessionManager {

    private static final int MAX_CHANNELS_PER_SESSION = 5;

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> userSessions =
            new ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>();

    private final ConcurrentHashMap<String, Sinks.Many<String>> sessionSinks =
            new ConcurrentHashMap<String, Sinks.Many<String>>();

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap =
            new ConcurrentHashMap<String, WebSocketSession>();

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> sessionChannels =
            new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> channelSessions =
            new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();

    public void register(String userId, WebSocketSession session) {
        userSessions.compute(userId, (key, sessions) -> {
            if (sessions == null) {
                sessions = new CopyOnWriteArraySet<WebSocketSession>();
            }
            sessions.add(session);
            return sessions;
        });
        sessionSinks.put(session.getId(), Sinks.many().unicast().onBackpressureBuffer());
        sessionMap.put(session.getId(), session);
        log.info("WebSocket session registered, userId={}, sessionId={}, totalSessions={}",
                userId, session.getId(), countAll());
    }

    public void remove(String userId, String sessionId) {
        userSessions.compute(userId, (key, sessions) -> {
            if (sessions == null) {
                return null;
            }
            sessions.removeIf(session -> session.getId().equals(sessionId));
            return sessions.isEmpty() ? null : sessions;
        });
        Sinks.Many<String> sink = sessionSinks.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        sessionMap.remove(sessionId);
        unsubscribeAll(sessionId);
        log.info("WebSocket session removed, userId={}, sessionId={}, totalSessions={}",
                userId, sessionId, countAll());
    }

    public Flux<String> outbound(String sessionId) {
        Sinks.Many<String> sink = sessionSinks.get(sessionId);
        return sink == null ? Flux.<String>empty() : sink.asFlux();
    }

    public boolean hasOnlineSessions(String userId) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public void pushToUser(String userId, String jsonMsg) {
        if (StringUtils.isBlank(userId) || jsonMsg == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            pushToSession(session.getId(), jsonMsg);
        }
    }

    public void broadcastAll(String jsonMsg) {
        if (jsonMsg == null || sessionSinks.isEmpty()) {
            return;
        }
        sessionSinks.forEach((sessionId, sink) -> emit(sessionId, sink, jsonMsg));
    }

    public void pushToSession(String sessionId, String jsonMsg) {
        Sinks.Many<String> sink = sessionSinks.get(sessionId);
        if (sink == null || jsonMsg == null) {
            return;
        }
        emit(sessionId, sink, jsonMsg);
    }

    public void pushToChannel(String channel, String jsonMsg) {
        if (StringUtils.isBlank(channel) || jsonMsg == null) {
            return;
        }
        CopyOnWriteArraySet<String> sessionIds = channelSessions.get(channel);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        for (String sessionId : sessionIds) {
            pushToSession(sessionId, jsonMsg);
        }
    }

    public void subscribe(String sessionId, Set<String> channels) {
        if (StringUtils.isBlank(sessionId) || channels == null || channels.isEmpty()) {
            return;
        }
        CopyOnWriteArraySet<String> owned = sessionChannels.computeIfAbsent(sessionId, key -> new CopyOnWriteArraySet<String>());
        for (String channel : channels.stream().filter(StringUtils::isNotBlank).toList()) {
            if (owned.size() >= MAX_CHANNELS_PER_SESSION) {
                log.warn("WebSocket channel limit reached, sessionId={}", sessionId);
                break;
            }
            if (owned.add(channel)) {
                channelSessions.computeIfAbsent(channel, key -> new CopyOnWriteArraySet<String>()).add(sessionId);
            }
        }
    }

    public void unsubscribe(String sessionId, Set<String> channels) {
        if (StringUtils.isBlank(sessionId) || channels == null || channels.isEmpty()) {
            return;
        }
        CopyOnWriteArraySet<String> owned = sessionChannels.get(sessionId);
        if (owned == null) {
            return;
        }
        for (String channel : channels.stream().filter(StringUtils::isNotBlank).toList()) {
            removeOwnedChannel(sessionId, owned, channel);
        }
    }

    private void removeOwnedChannel(String sessionId, CopyOnWriteArraySet<String> owned, String channel) {
        if (owned.remove(channel)) {
            channelSessions.compute(channel, (key, sessionIds) -> {
                if (sessionIds == null) {
                    return null;
                }
                sessionIds.remove(sessionId);
                return sessionIds.isEmpty() ? null : sessionIds;
            });
        }
    }

    public int countAll() {
        return sessionMap.size();
    }

    private void unsubscribeAll(String sessionId) {
        CopyOnWriteArraySet<String> owned = sessionChannels.remove(sessionId);
        if (owned == null) {
            return;
        }
        for (String channel : owned) {
            channelSessions.compute(channel, (key, sessionIds) -> {
                if (sessionIds == null) {
                    return null;
                }
                sessionIds.remove(sessionId);
                return sessionIds.isEmpty() ? null : sessionIds;
            });
        }
    }

    private void emit(String sessionId, Sinks.Many<String> sink, String jsonMsg) {
        Sinks.EmitResult result = sink.tryEmitNext(jsonMsg);
        if (result.isFailure()) {
            log.warn("WebSocket push failed, sessionId={}, result={}", sessionId, result);
        }
    }
}
