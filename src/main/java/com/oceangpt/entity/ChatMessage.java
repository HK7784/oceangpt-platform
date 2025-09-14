package com.oceangpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "message_type")
    private String messageType;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    // 构造函数
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String sessionId, String message, String userId, String messageType) {
        this();
        this.sessionId = sessionId;
        this.message = message;
        this.userId = userId;
        this.messageType = messageType;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", message='" + message + '\'' +
                ", userId='" + userId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}