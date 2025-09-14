package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "聊天响应")
public class ChatResponse {
    
    @JsonProperty("message")
    @Schema(description = "AI回复消息", example = "海洋pH值的变化对海洋生态系统有重要影响...")
    private String message;
    
    @JsonProperty("sessionId")
    @Schema(description = "会话ID", example = "session_123456")
    private String sessionId;
    
    @JsonProperty("messageId")
    @Schema(description = "消息ID", example = "msg_789")
    private String messageId;
    
    @JsonProperty("timestamp")
    @Schema(description = "响应时间戳")
    private LocalDateTime timestamp;
    
    @JsonProperty("success")
    @Schema(description = "请求是否成功", example = "true")
    private boolean success = true;
    
    @JsonProperty("confidence")
    @Schema(description = "回答置信度", example = "0.95")
    private Double confidence;
    
    @JsonProperty("messageType")
    @Schema(description = "消息类型", example = "text")
    private String messageType = "text";
    
    @JsonProperty("supportingEvidence")
    @Schema(description = "支持证据")
    private List<String> supportingEvidence;
    
    @JsonProperty("recommendations")
    @Schema(description = "相关建议")
    private List<String> recommendations;
    
    @JsonProperty("relatedData")
    @Schema(description = "相关数据")
    private Map<String, Object> relatedData;
    
    @JsonProperty("followUpQueries")
    @Schema(description = "后续问题建议")
    private List<String> followUpQueries;
    
    @JsonProperty("technicalDetails")
    @Schema(description = "技术细节")
    private Map<String, Object> technicalDetails;
    
    @JsonProperty("sources")
    @Schema(description = "信息来源")
    private List<String> sources;
    
    @JsonProperty("attachments")
    @Schema(description = "附件信息")
    private List<AttachmentInfo> attachments;
    
    @JsonProperty("steps")
    @Schema(description = "处理步骤")
    private List<String> steps;

    // 构造函数
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
        this.messageId = "msg_" + System.currentTimeMillis();
    }

    public ChatResponse(String message, String sessionId) {
        this();
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<String> getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence(List<String> supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public Map<String, Object> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(Map<String, Object> relatedData) {
        this.relatedData = relatedData;
    }

    public List<String> getFollowUpQueries() {
        return followUpQueries;
    }

    public void setFollowUpQueries(List<String> followUpQueries) {
        this.followUpQueries = followUpQueries;
    }

    public Map<String, Object> getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(Map<String, Object> technicalDetails) {
        this.technicalDetails = technicalDetails;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<AttachmentInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentInfo> attachments) {
        this.attachments = attachments;
    }
    
    public List<String> getSteps() {
        return steps;
    }
    
    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
    
    // 便捷方法
    public void addStep(String step) {
        if (this.steps == null) {
            this.steps = new java.util.ArrayList<>();
        }
        this.steps.add(step);
    }
    
    public void addData(String key, Object value) {
        if (this.relatedData == null) {
            this.relatedData = new java.util.HashMap<>();
        }
        this.relatedData.put(key, value);
    }
    
    public Object getData(String key) {
        return this.relatedData != null ? this.relatedData.get(key) : null;
    }
    
    /**
     * 构建器模式
     */
    public static class Builder {
        private ChatResponse response;
        
        public Builder() {
            this.response = new ChatResponse();
        }
        
        public Builder setSuccess(boolean success) {
            response.setSuccess(success);
            return this;
        }
        
        public Builder setMessage(String message) {
            response.setMessage(message);
            return this;
        }
        
        public Builder addStep(String step) {
            response.addStep(step);
            return this;
        }
        
        public Builder addData(String key, Object value) {
            response.addData(key, value);
            return this;
        }
        
        public Builder setSessionId(String sessionId) {
            response.setSessionId(sessionId);
            return this;
        }
        
        public Builder setConfidence(Double confidence) {
            response.setConfidence(confidence);
            return this;
        }
        
        public ChatResponse build() {
            return response;
        }
    }

    // 内部类：附件信息
    public static class AttachmentInfo {
        @JsonProperty("type")
        @Schema(description = "附件类型", example = "chart")
        private String type;
        
        @JsonProperty("url")
        @Schema(description = "附件URL", example = "/api/charts/water-quality-trend")
        private String url;
        
        @JsonProperty("title")
        @Schema(description = "附件标题", example = "水质趋势图")
        private String title;
        
        @JsonProperty("description")
        @Schema(description = "附件描述")
        private String description;

        // 构造函数
        public AttachmentInfo() {}

        public AttachmentInfo(String type, String url, String title) {
            this.type = type;
            this.url = url;
            this.title = title;
        }

        // Getter和Setter方法
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                ", confidence=" + confidence +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}