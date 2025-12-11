package com.oceangpt.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的会话级记忆组件
 */
public class AgentMemory {
    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public Map<String, Object> getMemory(String sessionId) {
        return store.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    public void set(String sessionId, String key, Object value) {
        getMemory(sessionId).put(key, value);
    }

    public Object get(String sessionId, String key) {
        return getMemory(sessionId).get(key);
    }
}
