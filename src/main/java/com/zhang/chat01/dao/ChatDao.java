package com.zhang.chat01.repository;

import com.zhang.chat01.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatDao extends JpaRepository<ConversationHistory, Long> {
    List<ConversationHistory> findByUserIdAndConversationIdOrderByCreatedAtAsc(String userId, String conversationId);
    void deleteByUserIdAndConversationId(String userId, String conversationId);
    void deleteByUserId(String userId);
    List<ConversationHistory> findByUserId(String userId);
}
