package com.example.superaiagents.memory.summary;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摘要服务 - 处理对话压缩和偏好提取
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    @Resource
    private ChatClient chatClient;

    /**
     * 生成对话摘要
     */
    public String generateSummary(List<Message> messages) {
        List<Message> userMessages = messages.stream()
                .filter(msg -> "USER".equalsIgnoreCase(msg.getMessageType().name()))
                .collect(Collectors.toList());
        if (userMessages.isEmpty()) {
            return "用户暂无明确需求";
        }

        StringBuilder sb = new StringBuilder();
        for (Message msg : userMessages) {
            sb.append("- ").append(msg.getText()).append("\n");
        }

        String prompt = """
                只根据下面的【用户消息】总结用户本人明确提出的问题、需求、偏好和约束。
                不要总结或复述 AI 的回答、推荐结果、商家名称、菜品描述。
                不要把 AI 的建议当成用户偏好；只有用户明确表达的内容才可以写入。
                50字以内，输出一句自然语言摘要。

                【用户消息】
                %s
                """.formatted(sb);

        try {
            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return summary != null ? summary : "对话摘要";
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
            return "对话摘要";
        }
    }

    /**
     * 从摘要中提取用户偏好
     */
    public String extractPreferences(String summary) {
        String prompt = """
            只从以下【用户需求摘要】中提取用户明确表达的美食偏好，输出 JSON 格式：
            {
              "taste": {"preferred": ["甜", "辣"], "disliked": ["苦"]},
              "restrictions": {"allergies": ["海鲜"], "avoidIngredients": ["香菜"]},
              "cookingLevel": "intermediate",
              "healthGoals": ["减脂"],
              "favoriteCuisines": ["川菜", "粤菜"]
            }

            规则：
            - 只提取用户明确表达的偏好、忌口、过敏、健康目标、菜系偏好。
            - 不要根据 AI 推荐的餐厅、菜品或描述反推用户偏好。
            - 如果没有明确信息，字段设为空数组或默认值。

            【用户需求摘要】：
            """ + summary;

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (result != null && result.contains("{")) {
                int start = result.indexOf("{");
                int end = result.lastIndexOf("}") + 1;
                return result.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("提取用户偏好失败: {}", e.getMessage());
        }
        return null;
    }
}
