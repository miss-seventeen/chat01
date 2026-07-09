package com.zhang.chat01.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
//import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@RestController
@RequestMapping("minimax")
public class MiniMaxController {
    @Resource
    private ChatModel chatModel;

    // ✅ 存 ChatClient 实例（不是 Builder）
    private final ConcurrentHashMap<String, ChatClient> userClients = new ConcurrentHashMap<>();
    
    // ✅ 改为二级结构：userId -> (conversationId -> 对话历史)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, java.util.List<Message>>> conversationHistories = new ConcurrentHashMap<>();

    public  MiniMaxController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    //第一种全量返回
    @GetMapping("/call")
    public Mono<String> call(@RequestParam("question") String question) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();  // 改这里 ✅
        return Mono.fromCallable(() ->
                chatClient.prompt(new Prompt(question))
                        .call()
                        .content()
        ).subscribeOn(Schedulers.boundedElastic());
    }

    //第二种:打字机效果
    @GetMapping(value = "/stream/str", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamStr(@RequestParam("question") String question) {
        ChatClient chatClient = ChatClient.create(chatModel);
        return chatClient.prompt(question)
                .stream()
                .content();  // 返回 Flux<String>，每个字单独推送
    }

    //第三种动态参数配置，告诉AI需要做什么,短文本
    @GetMapping("/call/prompt")
    public Mono<String> callPrompt(@RequestParam("player") String player) {
        ChatClient chatClient = ChatClient.create(chatModel);
        return Mono.fromCallable(() -> chatClient
                .prompt()
                .user(u ->
                        u.text("请输出NBA球员{player}的最高得分")
                                .param("player", player)
                )  // 动态参数
                .call()
                .content()
        ).subscribeOn(Schedulers.boundedElastic());
    }

    //第四种:打字机效果+长文本
    @GetMapping(value = "/stream/param", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMetadata(
            @RequestParam("question") String question,
            @RequestParam("userId") String userId,
            @RequestParam(value = "conversationId", required = false) String conversationId  // ✅ 新增：会话ID
    ) {
        // ✅ 如果没有传conversationId，使用默认值
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "default";
        }

        ChatClient client = userClients.computeIfAbsent(userId,
                id -> ChatClient.create(chatModel)
        );

        // ✅ 获取该用户的会话集合
        ConcurrentHashMap<String, java.util.List<Message>> userConversations =
                conversationHistories.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());

        // ✅ 获取该会话的历史对话
        java.util.List<Message> history = userConversations.computeIfAbsent(conversationId,
                id -> new java.util.ArrayList<>()
        );

        // ✅ 将当前问题加入历史
        history.add(new UserMessage(question));

        return client.prompt()
                .system(s -> s.text("你是一个助手，只对话，不调用工具"))
                .messages(history)
                .stream()
                .content()
                .collectList()
                .flatMapMany(fullResponse -> {
                    // ✅ 收集完整响应后，合并成一个字符串添加到历史
                    String completeResponse = String.join("", fullResponse);
                    history.add(new AssistantMessage(completeResponse));
                    return Flux.fromIterable(fullResponse);
                });
    }

    // 删除某个会话的历史
    @GetMapping("/clear/conversation")
    public Mono<String> clearConversation(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId
    ) {
        ConcurrentHashMap<String, java.util.List<Message>> userConversations = conversationHistories.get(userId);
        if (userConversations != null) {
            userConversations.remove(conversationId);
        }
        return Mono.just("已清除用户 " + userId + " 的会话 " + conversationId);
    }

    // 查看用户的所有会话
    @GetMapping("/conversations")
    public Mono<java.util.Set<String>> listConversations(@RequestParam("userId") String userId) {
        ConcurrentHashMap<String, java.util.List<Message>> userConversations = conversationHistories.get(userId);
        if (userConversations == null) {
            return Mono.just(java.util.Collections.emptySet());
        }
        return Mono.just(userConversations.keySet());
    }

    // ✅ 新增：查看指定会话的完整对话历史
    @GetMapping("/history")
    public Mono<java.util.List<Map<String, String>>> getConversationHistory(
            @RequestParam("userId") String userId,
            @RequestParam("conversationId") String conversationId
    ) {
        ConcurrentHashMap<String, java.util.List<Message>> userConversations = conversationHistories.get(userId);
        if (userConversations == null) {
            return Mono.just(java.util.Collections.emptyList());
        }
        
        java.util.List<Message> history = userConversations.get(conversationId);
        if (history == null || history.isEmpty()) {
            return Mono.just(java.util.Collections.emptyList());
        }
        
        // ✅ 转换为可读格式
        java.util.List<Map<String, String>> result = new java.util.ArrayList<>();
        for (Message msg : history) {
            Map<String, String> messageMap = new java.util.HashMap<>();
            messageMap.put("role", msg.getMessageType().toString());
            messageMap.put("content", msg.getText());
            result.add(messageMap);
        }
        
        return Mono.just(result);
    }

    // ✅ 新增：清空所有会话历史
    @GetMapping("/clear/all")
    public Mono<String> clearAllHistory(@RequestParam("userId") String userId) {
        conversationHistories.remove(userId);
        userClients.remove(userId);
        return Mono.just("已清除用户 " + userId + " 的所有会话历史");
    }


}
