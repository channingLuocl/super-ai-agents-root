package com.example.superaiagents.advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话历史摘要压缩 Advisor
 * <p>
 * 功能：当对话历史过长时，自动将早期对话压缩为摘要，
 * 保留关键信息的同时减少 token 消耗
 */
public class SummaryCompressAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 保留最近 N 轮对话（每轮包含用户+AI，即2条消息）
     */
    private static final int KEEP_RECENT_ROUNDS = 3;

    /**
     * 触发摘要的最小消息数（超过此值才进行压缩）
     */
    private static final int MIN_MESSAGES_FOR_SUMMARY = 7;

    private final ChatClient summaryChatClient;

    public SummaryCompressAdvisor(ChatModel chatModel) {
        this.summaryChatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 10; // 在 ReReadingAdvisor(0) 之后执行，让重读优化基于压缩后的上下文
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        ChatClientRequest compressedRequest = before(chatClientRequest);
        return chain.nextCall(compressedRequest);
    }

    /**
     * 处理请求前：判断是否需要压缩，需要则生成摘要
     */
    private ChatClientRequest before(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();

        // 消息数不足，不压缩
        if (messages.size() < MIN_MESSAGES_FOR_SUMMARY) {
            return request;
        }

        // 1. 分离系统消息
        List<Message> systemMessages = messages.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .collect(Collectors.toList());

        // 2. 获取需要压缩的旧消息（去掉系统消息和最近保留的部分）
        int recentCount = KEEP_RECENT_ROUNDS * 2;
        List<Message> oldMessages = messages.subList(
                systemMessages.size(),
                messages.size() - recentCount
        );

        // 3. 生成摘要
        String summary = generateSummary(oldMessages);

        // 4. 保留系统消息 + 摘要消息 + 最近对话
        List<Message> result = new ArrayList<>(systemMessages);
        result.add(new AssistantMessage("【对话摘要】：" + summary));
        result.addAll(messages.subList(messages.size() - recentCount, messages.size()));

        Prompt compressedPrompt = new Prompt(result, request.prompt().getOptions());
        return new ChatClientRequest(compressedPrompt, request.context());
    }

    /**
     * 生成对话摘要
     */
    private String generateSummary(List<Message> oldMessages) {
        if (oldMessages.isEmpty()) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        for (Message msg : oldMessages) {
            if (msg.getMessageType() == MessageType.USER) {
                sb.append("用户：").append(msg.getText()).append("\n");
            } else if (msg.getMessageType() == MessageType.ASSISTANT) {
                sb.append("AI：").append(msg.getText()).append("\n");
            }
        }

        String historyText = sb.toString();

        // 调用 LLM 生成摘要
        String summary = summaryChatClient.prompt()
                .user("请简要概括以下对话内容，保留关键信息和用户需求（不超过100字）：\n" + historyText)
                .call()
                .content();

        return summary != null ? summary.trim() : "无";
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        ChatClientRequest compressedRequest = before(chatClientRequest);
        return chain.nextStream(compressedRequest);
    }
}
