package com.oceangpt.controller;

import com.oceangpt.dto.ChatRequest;
import com.oceangpt.dto.ChatResponse;
import com.oceangpt.service.ChatService;
import com.oceangpt.service.EnhancedChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "聊天对话API")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private EnhancedChatService enhancedChatService;

    @PostMapping("/message")
    @Operation(summary = "发送聊天消息", description = "发送消息并获取AI回复")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            // 使用增强的聊天服务处理消息
            ChatResponse response = enhancedChatService.processMessage(
                request.getSessionId(), 
                request.getMessage(), 
                request.getUserId() != null ? request.getUserId() : "anonymous"
            );
            response.setSessionId(request.getSessionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉，处理您的消息时出现了错误：" + e.getMessage());
            errorResponse.setSuccess(false);
            errorResponse.setSessionId(request.getSessionId());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/message/legacy")
    @Operation(summary = "发送聊天消息(旧版)", description = "使用旧版聊天服务发送消息")
    public ResponseEntity<ChatResponse> sendMessageLegacy(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.processMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉，处理您的消息时出现了错误：" + e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取聊天历史", description = "根据会话ID获取聊天历史记录")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable String sessionId) {
        try {
            List<ChatResponse> history = chatService.getChatHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/history/{sessionId}")
    @Operation(summary = "清除聊天历史", description = "清除指定会话的聊天历史记录")
    public ResponseEntity<Void> clearChatHistory(@PathVariable String sessionId) {
        try {
            chatService.clearChatHistory(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "获取所有活跃的聊天会话")
    public ResponseEntity<List<String>> getActiveSessions() {
        try {
            List<String> sessions = chatService.getActiveSessions();
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}