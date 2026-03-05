package com.peach.message.core.compont;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodic cleanup and metrics for websocket sessions.
 */
@Slf4j
@Component
public class WebSocketMaintenanceTask {

    @Scheduled(fixedDelayString = "${peach.websocket.cleanup-interval-ms:60000}")
    public void cleanupAndMetrics() {
        AtomicInteger activeConnections = new AtomicInteger(0);
        AtomicInteger removedConnections = new AtomicInteger(0);

        Context.CACHE.forEach((type, cache) -> {
            if (cache == null) {
                return;
            }
            cache.prune();

            List<String> toRemove = new ArrayList<String>();
            for (WebSocketServer server : cache) {
                if (server == null || !server.isOpen()) {
                    if (server != null && server.getSessionId() != null) {
                        toRemove.add(server.getSessionId());
                    }
                } else {
                    activeConnections.incrementAndGet();
                }
            }

            for (String sessionId : toRemove) {
                cache.remove(sessionId);
                removedConnections.incrementAndGet();
            }

            // remove empty type buckets
            if (cache.isEmpty()) {
                Context.CACHE.remove(type);
            }
        });

        log.info("WebSocket metrics: typeCount={}, activeConnections={}, removedConnections={}",
                Context.CACHE.size(), activeConnections.get(), removedConnections.get());
    }
}
