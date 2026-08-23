package com.peach.message.websocket;

import lombok.RequiredArgsConstructor;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peach.message.common.MessageConst;
import com.peach.message.common.enums.MessageEnum;
import com.peach.message.dto.WebSocketMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 响应式 WebSocket 处理器
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler implements WebSocketHandler {

    private static final int PING_INTERVAL_SECONDS = 30;

    private static final int TOKEN_CHECK_INTERVAL_SECONDS = 30;

    private static final Duration FORCE_LOGOUT_FLUSH_DELAY = Duration.ofMillis(200);

    private static final CloseStatus CLOSE_TOKEN_INVALID = new CloseStatus(4001, "token invalid or expired");

        private final WebSocketSessionManager sessionManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = extractToken(session);
        if (StringUtils.isBlank(token)) {
            log.warn("WebSocket missing token, sessionId={}", session.getId());
            return session.close(CLOSE_TOKEN_INVALID);
        }
        return Mono.fromCallable(() -> StpUtil.getLoginIdByToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(loginId -> {
                    if (loginId == null) {
                        log.warn("WebSocket token not found, sessionId={}", session.getId());
                        return session.close(CLOSE_TOKEN_INVALID);
                    }
                    return doHandle(session, String.valueOf(loginId), token);
                })
                .onErrorResume(SaTokenException.class, e -> {
                    log.warn("WebSocket token validation failed, sessionId={}, msg={}", session.getId(), e.getMessage());
                    return session.close(CLOSE_TOKEN_INVALID);
                });
    }

    private Mono<Void> doHandle(WebSocketSession session, String userId, String token) {
        sessionManager.register(userId, session);
        Flux<WebSocketMessage> pingFlux = Flux.interval(Duration.ofSeconds(PING_INTERVAL_SECONDS))
                .map(tick -> session.pingMessage(bufferFactory -> bufferFactory.wrap(new byte[0])));
        Flux<WebSocketMessage> textFlux = sessionManager.outbound(session.getId())
                .map(session::textMessage);
        Mono<Void> send = session.send(Flux.merge(pingFlux, textFlux));
        Mono<Void> receive = session.receive()
                .doOnNext(message -> {
                    if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
                        handleInboundText(session.getId(), userId, message.getPayloadAsText());
                    }
                })
                .then();
        Mono<Void> tokenGuard = Flux.interval(Duration.ofSeconds(TOKEN_CHECK_INTERVAL_SECONDS))
                .concatMap(tick -> Mono.fromCallable(() -> StpUtil.getLoginIdByToken(token))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(loginId -> {
                            if (loginId == null) {
                                return forceLogout(session, new TokenExpiredException("token expired"));
                            }
                            return Mono.<Void>empty();
                        })
                        .onErrorResume(SaTokenException.class, e -> forceLogout(session, e)))
                .then();
        return Mono.firstWithSignal(send, receive, tokenGuard)
                .doFinally(signal -> sessionManager.remove(userId, session.getId()))
                .onErrorResume(e -> e instanceof TokenExpiredException || e instanceof SaTokenException,
                        e -> Mono.empty());
    }

    private Mono<Void> forceLogout(WebSocketSession session, Throwable cause) {
        WebSocketMessageDTO message = new WebSocketMessageDTO();
        message.setMode(MessageEnum.WsPushMode.SINGLE);
        message.setType(MessageEnum.WebSocketEventType.KICK_OUT.getCode());
        message.setTimestamp(System.currentTimeMillis());
        sessionManager.pushToSession(session.getId(), JSON.toJSONString(message));
        return Mono.delay(FORCE_LOGOUT_FLUSH_DELAY)
                .then(session.close(CLOSE_TOKEN_INVALID))
                .then(Mono.error(cause));
    }

    private void handleInboundText(String sessionId, String userId, String payload) {
        if (StringUtils.isBlank(payload)) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(payload);
            if (root == null) {
                return;
            }
            String action = root.getString("action");
            JSONArray channels = root.getJSONArray("channels");
            if (StringUtils.isBlank(action) || channels == null || channels.isEmpty()) {
                return;
            }
            Set<String> allowed = filterAllowedChannels(userId, channels);
            if (allowed.isEmpty()) {
                return;
            }
            if ("subscribe".equalsIgnoreCase(action)) {
                sessionManager.subscribe(sessionId, allowed);
            } else if ("unsubscribe".equalsIgnoreCase(action)) {
                sessionManager.unsubscribe(sessionId, allowed);
            }
        } catch (Exception e) {
            log.debug("Ignore WebSocket inbound text, userId={}, err={}", userId, e.getMessage());
        }
    }

    private Set<String> filterAllowedChannels(String userId, JSONArray channels) {
        Set<String> allowed = new HashSet<String>();
        for (int i = 0; i < channels.size(); i++) {
            String channel = channels.getString(i);
            if (StringUtils.isBlank(channel)) {
                continue;
            }
            channel = channel.trim();
            if (channel.startsWith("user:")) {
                String idPart = channel.substring("user:".length());
                if (userId.equals(idPart)) {
                    allowed.add(channel);
                }
            } else if (channel.startsWith(MessageConst.DEFAULT_WEBSOCKET_CHANNEL + ":")) {
                allowed.add(channel);
            }
        }
        return allowed;
    }

    private String extractToken(WebSocketSession session) {
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();
        String token = params.getFirst("satoken");
        if (StringUtils.isBlank(token)) {
            token = params.getFirst("token");
        }
        if (StringUtils.isBlank(token)) {
            token = params.getFirst("Authorization");
        }
        if (StringUtils.isBlank(token)) {
            token = session.getHandshakeInfo().getHeaders().getFirst("satoken");
        }
        if (StringUtils.isBlank(token)) {
            token = session.getHandshakeInfo().getHeaders().getFirst("Authorization");
        }
        if (StringUtils.isBlank(token)) {
            token = session.getHandshakeInfo().getHeaders().getFirst("Sec-WebSocket-Protocol");
        }
        return StringUtils.trimToNull(token);
    }

    private static class TokenExpiredException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TokenExpiredException(String message) {
            super(message);
        }
    }
}
