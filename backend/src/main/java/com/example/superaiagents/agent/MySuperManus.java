package com.example.superaiagents.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class MySuperManus extends ToolCallAgent {

    public MySuperManus(ToolCallback[] allTools, ChatModel chatModel) {
        super(allTools);
        this.setName("mySuperManus");
        String SYSTEM_PROMPT = """
                You are MySuperManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
//        你是 MySuperManus，一名全能的 AI 助手，旨在解决用户提出的任何任务。你拥有各种工具，可以调用这些工具来高效完成复杂的请求。
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
//        根据用户需求，主动选择最合适的工具或工具组合。
//        对于复杂任务，你可以将问题分解，并逐步使用不同的工具来解决。
//        在使用每个工具后，清楚地解释执行结果并提出下一步建议。
//        如果你想在任何时候停止交互，请使用 `terminate` 工具/函数调用。
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(chatModel)
//                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}