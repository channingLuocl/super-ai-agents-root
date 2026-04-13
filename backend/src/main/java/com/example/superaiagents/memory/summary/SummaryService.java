package com.example.superaiagents.memory.summary;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

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
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append(msg.getMessageType().name().toLowerCase())
              .append(": ")
              .append(msg.getText())
              .append("\n");
        }

        String prompt = "请简要概括以下对话的要点，提取用户的关键偏好和需求（50字以内）：\n" + sb.toString();

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
            从以下对话摘要中提取用户美食偏好，输出 JSON 格式：
            {
              "taste": {"preferred": ["甜", "辣"], "disliked": ["苦"]},
              "restrictions": {"allergies": ["海鲜"], "avoidIngredients": ["香菜"]},
              "cookingLevel": "intermediate",
              "healthGoals": ["减脂"],
              "favoriteCuisines": ["川菜", "粤菜"]
            }

            如果没有明确信息，字段设为空数组或默认值。

            对话摘要：
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
