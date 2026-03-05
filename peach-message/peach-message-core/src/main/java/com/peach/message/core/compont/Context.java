package com.peach.message.core.compont;

import cn.hutool.cache.impl.TimedCache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:09
 */
public class Context {
    // 这里的 Cache 依然是本地的，用于持有实际的 WebSocket Session
    public static ConcurrentHashMap<String, TimedCache<String, WebSocketServer>> CACHE = new ConcurrentHashMap<>();
}
