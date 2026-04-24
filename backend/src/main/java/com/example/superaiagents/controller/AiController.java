package com.example.superaiagents.controller;


import com.example.superaiagents.agent.MySuperManus;
import com.example.superaiagents.app.FoodApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private FoodApp foodApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel chatModel;

    /**
     * 同步调用 AI 美食助手
     */
    @GetMapping("/food/chat")
    public String doChatWithFoodApp(String message, String chatId) {
        return foodApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 美食助手
     */
    @GetMapping(value = "/food/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithFoodAppStream(String message, String chatId) {
        return foodApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 RAG 美食知识库问答
     */
    @GetMapping(value = "/food/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithRagStream(String message, String chatId) {
        return foodApp.doChatWithRagStream(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MySuperManus yuManus = new MySuperManus(allTools, chatModel);
        return yuManus.runStream(message);
    }
}