package com.oceangpt.controller;

import com.oceangpt.dto.ChatRequest;
import com.oceangpt.dto.ChatResponse;
import com.oceangpt.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 处理聊天消息
     * 客户端发送到 /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatRequest chatRequest, SimpMessageHeaderAccessor headerAccessor) {
        logger.info("收到WebSocket聊天消息: sessionId={}, message={}", 
                   chatRequest.getSessionId(), chatRequest.getMessage());
        
        try {
            // 处理消息
            ChatResponse response = chatService.processMessage(chatRequest);
            
            // 发送响应到特定会话
            String destination = "/topic/chat." + chatRequest.getSessionId();
            messagingTemplate.convertAndSend(destination, response);
            
            logger.info("WebSocket消息处理完成: sessionId={}, destination={}", 
                       chatRequest.getSessionId(), destination);
            
        } catch (Exception e) {
            logger.error("WebSocket消息处理失败: {}", e.getMessage(), e);
            
            // 发送错误响应
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSessionId(chatRequest.getSessionId());
            errorResponse.setMessage("抱歉，处理您的消息时出现了错误，请稍后重试。");
            errorResponse.setSuccess(false);
            
            String destination = "/topic/chat." + chatRequest.getSessionId();
            messagingTemplate.convertAndSend(destination, errorResponse);
        }
    }
    
    /**
     * 用户加入聊天
     * 客户端发送到 /app/chat.addUser
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatRequest chatRequest, SimpMessageHeaderAccessor headerAccessor) {
        // 在WebSocket会话中添加用户信息
        headerAccessor.getSessionAttributes().put("sessionId", chatRequest.getSessionId());
        headerAccessor.getSessionAttributes().put("userId", chatRequest.getUserId());
        
        logger.info("用户加入聊天: sessionId={}, userId={}", 
                   chatRequest.getSessionId(), chatRequest.getUserId());
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(chatRequest.getSessionId());
        response.setMessage("欢迎使用OceanGPT智能对话系统！我是您的海洋科学助手，有什么问题可以随时问我。");
        response.setMessageType("system");
        response.setSuccess(true);
        
        // 发送欢迎消息到特定会话
        String destination = "/topic/chat." + chatRequest.getSessionId();
        messagingTemplate.convertAndSend(destination, response);
        
        logger.info("用户加入确认消息已发送: sessionId={}, destination={}", 
                   chatRequest.getSessionId(), destination);
    }
    
    /**
     * 获取聊天历史
     * 客户端发送到 /app/chat.getHistory
     */
    @MessageMapping("/chat.getHistory")
    public void getChatHistory(@Payload ChatRequest chatRequest) {
        logger.info("获取聊天历史: sessionId={}", chatRequest.getSessionId());
        
        try {
            var history = chatService.getChatHistory(chatRequest.getSessionId());
            
            // 发送历史记录到特定会话
            String destination = "/topic/chat.history." + chatRequest.getSessionId();
            messagingTemplate.convertAndSend(destination, history);
            
        } catch (Exception e) {
            logger.error("获取聊天历史失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清除聊天历史
     * 客户端发送到 /app/chat.clearHistory
     */
    @MessageMapping("/chat.clearHistory")
    public void clearChatHistory(@Payload ChatRequest chatRequest) {
        logger.info("清除聊天历史: sessionId={}", chatRequest.getSessionId());
        
        try {
            chatService.clearChatHistory(chatRequest.getSessionId());
            
            // 发送确认消息
            ChatResponse response = new ChatResponse();
            response.setSessionId(chatRequest.getSessionId());
            response.setMessage("聊天历史已清除");
            response.setMessageType("system");
            response.setSuccess(true);
            
            String destination = "/topic/chat." + chatRequest.getSessionId();
            messagingTemplate.convertAndSend(destination, response);
            
        } catch (Exception e) {
            logger.error("清除聊天历史失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 用户离开聊天
     */
    @MessageMapping("/chat.leaveUser")
    public void leaveUser(@Payload ChatRequest chatRequest) {
        logger.info("用户离开聊天: sessionId={}, userId={}", 
                   chatRequest.getSessionId(), chatRequest.getUserId());
        
        // 可以在这里添加清理逻辑
    }
}