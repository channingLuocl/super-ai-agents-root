package com.example.superaiagents.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatConversationService {

    private static final Logger log = LoggerFactory.getLogger(ChatConversationService.class);

    private static final String CONVERSATION_IDS_KEY_PREFIX = "chat:conversation_ids:";
    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";
    private static final String CURRENT_CHAT_KEY_PREFIX = "chat:current:";
    private static final String DEFAULT_TITLE = "新对话";

    @Resource
    private JedisPooled jedisPooled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ChatConversation> getConversations(String userId) {
        List<String> chatIds = jedisPooled.zrevrange(conversationIdsKey(userId), 0, -1);
        List<ChatConversation> conversations = new ArrayList<>();
        for (String chatId : chatIds) {
            ChatConversation conversation = getConversation(userId, chatId);
            if (conversation == null) {
                jedisPooled.zrem(conversationIdsKey(userId), chatId);
                continue;
            }
            conversations.add(conversation);
        }
        return conversations;
    }

    public ChatConversation createConversation(String userId) {
        long now = System.currentTimeMillis();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(generateId());
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        setCurrentChatId(userId, conversation.getId());
        return conversation;
    }

    public ChatConversation getConversation(String userId, String chatId) {
        String json = jedisPooled.get(conversationKey(userId, chatId));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChatConversation.class);
        } catch (Exception e) {
            log.warn("Failed to parse chat conversation {} for user {}: {}", chatId, userId, e.getMessage());
            jedisPooled.del(conversationKey(userId, chatId));
            jedisPooled.zrem(conversationIdsKey(userId), chatId);
            return null;
        }
    }

    public ChatConversation updateMessages(String userId, String chatId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            deleteConversation(userId, chatId);
            return createDraftConversation(chatId);
        }

        long now = System.currentTimeMillis();
        ChatConversation conversation = getConversation(userId, chatId);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setId(chatId);
            conversation.setTitle(DEFAULT_TITLE);
            conversation.setCreatedAt(now);
        }

        conversation.setMessages(messages);
        conversation.setTitle(buildTitle(messages));
        conversation.setUpdatedAt(now);

        saveConversation(userId, conversation);
        setCurrentChatId(userId, chatId);
        return conversation;
    }

    public void deleteConversation(String userId, String chatId) {
        jedisPooled.del(conversationKey(userId, chatId));
        jedisPooled.zrem(conversationIdsKey(userId), chatId);
        if (chatId.equals(getCurrentChatId(userId))) {
            List<String> chatIds = jedisPooled.zrevrange(conversationIdsKey(userId), 0, 0);
            if (chatIds.isEmpty()) {
                jedisPooled.del(currentChatKey(userId));
            } else {
                setCurrentChatId(userId, chatIds.get(0));
            }
        }
    }

    public String getCurrentChatId(String userId) {
        return jedisPooled.get(currentChatKey(userId));
    }

    public void setCurrentChatId(String userId, String chatId) {
        jedisPooled.set(currentChatKey(userId), chatId);
    }

    private void saveConversation(String userId, ChatConversation conversation) {
        try {
            jedisPooled.set(conversationKey(userId, conversation.getId()), objectMapper.writeValueAsString(conversation));
            jedisPooled.zadd(conversationIdsKey(userId), conversation.getUpdatedAt(), conversation.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save chat conversation", e);
        }
    }

    private ChatConversation createDraftConversation(String chatId) {
        long now = System.currentTimeMillis();
        ChatConversation conversation = new ChatConversation();
        conversation.setId(chatId);
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        return conversation;
    }

    private String buildTitle(List<ChatMessage> messages) {
        if (messages == null) {
            return DEFAULT_TITLE;
        }
        return messages.stream()
                .filter(ChatMessage::isUser)
                .map(ChatMessage::getContent)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .map(this::truncateTitle)
                .orElse(DEFAULT_TITLE);
    }

    private String truncateTitle(String content) {
        String trimmed = content.trim();
        if (trimmed.length() <= 20) {
            return trimmed;
        }
        return trimmed.substring(0, 20) + "...";
    }

    private String conversationIdsKey(String userId) {
        return CONVERSATION_IDS_KEY_PREFIX + userId;
    }

    private String conversationKey(String userId, String chatId) {
        return CONVERSATION_KEY_PREFIX + userId + ":" + chatId;
    }

    private String currentChatKey(String userId) {
        return CURRENT_CHAT_KEY_PREFIX + userId;
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
