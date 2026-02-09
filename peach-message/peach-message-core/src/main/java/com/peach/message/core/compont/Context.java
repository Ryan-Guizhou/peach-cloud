package com.peach.message.core.compont;

import cn.hutool.cache.impl.TimedCache;

import java.util.concurrent.ConcurrentHashMap;

public class Context {
    // 这里的 Cache 依然是本地的，用于持有实际的 WebSocket Session
    public static ConcurrentHashMap<String, TimedCache<String, WebSocketServer>> CACHE = new ConcurrentHashMap<>();
}
