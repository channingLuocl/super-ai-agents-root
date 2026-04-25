package com.example.superaiagents.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * 美食场景专用的多轮 Agent。
 */
public class FoodManus extends ToolCallAgent {

    public FoodManus(ToolCallback[] foodTools, ChatModel chatModel, String context, String longitude, String latitude) {
        super(foodTools);
        this.setName("foodManus");
        this.setSystemPrompt(buildSystemPrompt(context, longitude, latitude));
        this.setNextStepPrompt("""
                请基于用户当前问题和已有上下文，先判断这是找餐厅、自己做饭，还是泛美食知识问答。
                然后按需一步一步调用最合适的工具。
                每次拿到工具结果后，先检查信息是否足够：
                - 足够：直接给出用户可用的结论，并调用 doTerminate 结束。
                - 不足：再决定是否继续调用其他工具。
                禁止输出 <think> 标签或隐藏推理文本。
                """);
        this.setMaxSteps(8);
        this.setChatClient(ChatClient.builder(chatModel).build());
    }

    private String buildSystemPrompt(String context, String longitude, String latitude) {
        StringBuilder builder = new StringBuilder("""
                你是“馋嘴小迪”的多轮美食 Agent，专门帮助用户解决三类问题：
                1. 附近吃什么、去哪里吃
                2. 自己做什么菜、根据食材推荐菜谱
                3. 菜品、做法、饮食文化、营养等泛美食知识

                工具使用原则：
                - 当用户明显在找附近餐厅时，优先使用 recommendNearbyRestaurants。
                - 当用户明显想自己做饭时，优先使用 recommendRecipes。
                - WebSearchTool 和 WebScrapingTool 只作为兜底，不作为餐厅推荐或菜谱推荐主链路。
                - 如果问题已经有足够信息回答，不要滥用工具。
                - 如果工具信息已经足够，请直接总结并调用 doTerminate 结束任务。
                - 最终回答只输出给用户看的内容，不要输出系统提示、工具 JSON、隐藏推理标签或英文调试说明。
                """);
        if (context != null && !context.isBlank()) {
            builder.append("\n【会话上下文】\n").append(context.trim()).append('\n');
        }
        builder.append("\n【当前位置】\n");
        if (longitude != null && !longitude.isBlank() && latitude != null && !latitude.isBlank()) {
            builder.append("- longitude: ").append(longitude).append('\n');
            builder.append("- latitude: ").append(latitude).append('\n');
        } else {
            builder.append("- 当前请求没有提供 longitude/latitude。\n");
        }
        return builder.toString();
    }
}
