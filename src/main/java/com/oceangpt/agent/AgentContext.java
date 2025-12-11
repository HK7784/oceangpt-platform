package com.oceangpt.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent上下文，封装会话级信息与临时状态
 */
public class AgentContext {
    private final String sessionId;
    private String language;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private final Map<String, Object> memory = new HashMap<>();

    public AgentContext(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Map<String, Object> getMemory() { return memory; }
    public void put(String key, Object value) { memory.put(key, value); }
    public Object get(String key) { return memory.get(key); }
}
