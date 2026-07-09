package com.zhang.chat01.controller;

import com.zhang.chat01.entity.ConversationHistory;
import com.zhang.chat01.service.ChatService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@Data
public class ChatController {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ChatService historyRepository;

    private final ConcurrentHashMap<String, ChatClient> userClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<Message>>> conversationHistories = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("question") String question,
            @RequestParam("userId") String userId,
            @RequestParam(value = "conversationId", required = false) String conversationId
    ) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "default";
        }

        ChatClient client = userClients.computeIfAbsent(userId, id -> ChatClient.create(chatModel));

        ConcurrentHashMap<String, List<Message>> userConversations =
                conversationHistories.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());

        List<Message> history = userConversations.get(conversationId);
        if (history == null) {
            history = loadHistoryFromDatabase(userId, conversationId);
            userConversations.put(conversationId, history);
        }

        history.add(new UserMessage(question));
        saveMessageToDatabase(userId, conversationId, "USER", question);

        String finalConversationId = conversationId;
        List<Message> finalHistory = history;
        return client.prompt()
                .system(s -> s.text("你是一个助手，只对话，不调用工具"))
                .messages(history)
                .stream()
                .content()
                .collectList()
                .flatMapMany(fullResponse -> {
                    String completeResponse = String.join("", fullResponse);
                    finalHistory.add(new AssistantMessage(completeResponse));
                    saveMessageToDatabase(userId, finalConversationId, "ASSISTANT", completeResponse);
                    return Flux.fromIterable(fullResponse);
                });
    }

    @GetMapping("/history")
    public Mono<List<Map<String, String>>> getHistory(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId
    ) {
        List<ConversationHistory> records =
                historyRepository.findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);

        if (records.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        List<Map<String, String>> result = records.stream().map(record -> {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("role", record.getMessageRole());
            messageMap.put("content", record.getMessageContent());
            messageMap.put("createdAt", record.getCreatedAt().toString());
            return messageMap;
        }).collect(Collectors.toList());

        return Mono.just(result);
    }

    @GetMapping("/conversations")
    public Mono<Set<String>> listConversations(@RequestParam("userId") String userId) {
        List<ConversationHistory> allRecords = historyRepository.findByUserId(userId);

        Set<String> conversationIds = allRecords.stream()
                .map(ConversationHistory::getConversationId)
                .collect(Collectors.toSet());

        return Mono.just(conversationIds);
    }

    @DeleteMapping("/conversation")
    public Mono<String> clearConversation(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId
    ) {
        ConcurrentHashMap<String, List<Message>> userConversations = conversationHistories.get(userId);
        if (userConversations != null) {
            userConversations.remove(conversationId);
        }

        historyRepository.deleteByUserIdAndConversationId(userId, conversationId);

        return Mono.just("已清除用户 " + userId + " 的会话 " + conversationId);
    }

    @DeleteMapping("/all")
    public Mono<String> clearAllHistory(@RequestParam("userId") String userId) {
        conversationHistories.remove(userId);
        userClients.remove(userId);
        historyRepository.deleteByUserId(userId);

        return Mono.just("已清除用户 " + userId + " 的所有会话历史");
    }

    private List<Message> loadHistoryFromDatabase(String userId, String conversationId) {
        List<ConversationHistory> records =
                historyRepository.findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);

        return records.stream()
                .map(record -> {
                    if ("USER".equals(record.getMessageRole())) {
                        return new UserMessage(record.getMessageContent());
                    } else {
                        return new AssistantMessage(record.getMessageContent());
                    }
                })
                .collect(Collectors.toList());
    }

    private void saveMessageToDatabase(String userId, String conversationId, String role, String content) {
        ConversationHistory record = new ConversationHistory();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setMessageRole(role);
        record.setMessageContent(content);
        record.setCreatedAt(LocalDateTime.now());
        historyRepository.save(record);
    }
}
