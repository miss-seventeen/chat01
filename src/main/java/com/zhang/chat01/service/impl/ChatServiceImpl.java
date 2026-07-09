package com.zhang.chat01.service.impl;

import com.zhang.chat01.entity.ConversationHistory;
import com.zhang.chat01.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhang.chat01.repository.ChatDao;
import java.util.List;

@Service
public abstract class ChatServiceImpl implements ChatService {

    @Autowired public ChatDao chatDao;

    @Override
    public  List<ConversationHistory> findByUserIdAndConversationIdOrderByCreatedAtAsc(String userId, String conversationId) {
        return chatDao.findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);
    }

    @Override
    public  void deleteByUserIdAndConversationId(String userId, String conversationId) {
        chatDao.deleteByUserIdAndConversationId(userId, conversationId);
    }

    @Override
    public void deleteByUserId(String userId) {
        chatDao.deleteByUserId(userId);
    }

    @Override
    public List<ConversationHistory> findByUserId(String userId) {
        return chatDao.findByUserId(userId);
    }

}
