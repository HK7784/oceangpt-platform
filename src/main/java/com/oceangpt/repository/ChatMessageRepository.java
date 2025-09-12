package com.oceangpt.repository;

import com.oceangpt.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 根据会话ID查找消息
     */
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    /**
     * 根据会话ID和时间范围查找消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.timestamp BETWEEN :startTime AND :endTime ORDER BY cm.timestamp ASC")
    List<ChatMessage> findBySessionIdAndTimestampBetween(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 根据用户ID查找消息
     */
    List<ChatMessage> findByUserIdOrderByTimestampDesc(String userId);
    
    /**
     * 根据消息类型查找消息
     */
    List<ChatMessage> findByMessageTypeOrderByTimestampDesc(String messageType);
    
    /**
     * 删除指定会话的所有消息
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 删除指定时间之前的消息
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
    
    /**
     * 统计指定会话的消息数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 查找最近的会话ID列表
     */
    @Query("SELECT DISTINCT cm.sessionId FROM ChatMessage cm ORDER BY MAX(cm.timestamp) DESC")
    List<String> findDistinctSessionIds();
}