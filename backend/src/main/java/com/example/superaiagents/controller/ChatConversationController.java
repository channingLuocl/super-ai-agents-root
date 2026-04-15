package com.example.superaiagents.controller;

import com.example.superaiagents.chat.ChatConversation;
import com.example.superaiagents.chat.ChatConversationService;
import com.example.superaiagents.chat.ChatMessage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatConversationController {

    @Resource
    private ChatConversationService chatConversationService;

    @GetMapping("/conversations")
    public List<ChatConversation> getConversations(@RequestParam(defaultValue = "default") String userId) {
        return chatConversationService.getConversations(userId);
    }

    @PostMapping("/conversations")
    public ChatConversation createConversation(@RequestParam(defaultValue = "default") String userId) {
        return chatConversationService.createConversation(userId);
    }

    @GetMapping("/conversations/{chatId}")
    public ChatConversation getConversation(@PathVariable String chatId,
                                            @RequestParam(defaultValue = "default") String userId) {
        return chatConversationService.getConversation(userId, chatId);
    }

    @PutMapping("/conversations/{chatId}/messages")
    public ChatConversation updateMessages(@PathVariable String chatId,
                                           @RequestBody List<ChatMessage> messages,
                                           @RequestParam(defaultValue = "default") String userId) {
        return chatConversationService.updateMessages(userId, chatId, messages);
    }

    @DeleteMapping("/conversations/{chatId}")
    public Map<String, Object> deleteConversation(@PathVariable String chatId,
                                                  @RequestParam(defaultValue = "default") String userId) {
        chatConversationService.deleteConversation(userId, chatId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentChatId", chatConversationService.getCurrentChatId(userId));
        return result;
    }

    @GetMapping("/current")
    public Map<String, Object> getCurrentChatId(@RequestParam(defaultValue = "default") String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("chatId", chatConversationService.getCurrentChatId(userId));
        return result;
    }

    @PutMapping("/current/{chatId}")
    public Map<String, Object> setCurrentChatId(@PathVariable String chatId,
                                                @RequestParam(defaultValue = "default") String userId) {
        chatConversationService.setCurrentChatId(userId, chatId);
        Map<String, Object> result = new HashMap<>();
        result.put("chatId", chatId);
        return result;
    }
}
