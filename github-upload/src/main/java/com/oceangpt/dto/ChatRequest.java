package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天请求")
public class ChatRequest {
    
    @JsonProperty("message")
    @Schema(description = "用户消息内容", example = "海洋中的pH值对生态系统有什么影响？")
    private String message;
    
    @JsonProperty("sessionId")
    @Schema(description = "会话ID", example = "session_123456")
    private String sessionId;
    
    @JsonProperty("userId")
    @Schema(description = "用户ID", example = "user_001")
    private String userId;
    
    @JsonProperty("language")
    @Schema(description = "语言设置", example = "zh")
    private String language = "zh";
    
    @JsonProperty("location")
    @Schema(description = "地理位置信息")
    private LocationInfo location;
    
    @JsonProperty("context")
    @Schema(description = "上下文信息")
    private String context;

    // 构造函数
    public ChatRequest() {}

    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    // Getter和Setter方法
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocationInfo getLocation() {
        return location;
    }

    public void setLocation(LocationInfo location) {
        this.location = location;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    // 内部类：位置信息
    public static class LocationInfo {
        @JsonProperty("latitude")
        @Schema(description = "纬度", example = "39.9042")
        private Double latitude;
        
        @JsonProperty("longitude")
        @Schema(description = "经度", example = "116.4074")
        private Double longitude;
        
        @JsonProperty("region")
        @Schema(description = "区域名称", example = "渤海")
        private String region;

        // 构造函数
        public LocationInfo() {}

        public LocationInfo(Double latitude, Double longitude, String region) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.region = region;
        }

        // Getter和Setter方法
        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", language='" + language + '\'' +
                ", location=" + location +
                ", context='" + context + '\'' +
                '}';
    }
}