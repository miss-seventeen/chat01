package com.zhang.chat01.service;

import com.zhang.chat01.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatService  extends JpaRepository<ConversationHistory, Long> {

    List<ConversationHistory> findByUserIdAndConversationIdOrderByCreatedAtAsc(String userId, String conversationId);
    void deleteByUserIdAndConversationId(String userId, String conversationId);
    void deleteByUserId(String userId);
    List<ConversationHistory> findByUserId(String userId);
}
