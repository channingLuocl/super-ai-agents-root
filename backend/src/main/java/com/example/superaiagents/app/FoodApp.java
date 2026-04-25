package com.example.superaiagents.app;

import com.example.superaiagents.agent.FoodManus;
import com.example.superaiagents.memory.MemoryManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class FoodApp {

    private static final String SYSTEM_PROMPT_PREFIX = "扮演深耕美食领域的专家\"小迪\"。开场向用户表明身份，告知用户可以询问任何美食相关问题。"
            + "围绕以下几个方面与用户交流：\n"
            + "- 菜谱推荐：根据用户的需求推荐菜品做法\n"
            + "- 食材知识：介绍各种食材的选购、保存和营养知识\n"
            + "- 烹饪技巧：分享烹饪技巧和注意事项\n"
            + "- 美食文化：介绍各地美食特色和饮食文化\n"
            + "- 餐厅推荐：推荐特色餐厅和小吃\n"
            + "引导用户详述需求，比如口味偏好、预算、场合等，以便给出专属的美食建议。\n\n";

    private final ChatModel chatModel;

    @Resource(name = "foodTools")
    private ToolCallback[] foodTools;

    @Resource
    private MemoryManager memoryManager;

    public FoodApp(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public SseEmitter doChatWithFoodAgentStream(String message, String chatId, String longitude, String latitude) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String context = memoryManager.getContextForAI(conversationId, resolveUserId(conversationId));

        log.info("FoodAgent入口切换为多轮Agent: conversationId={}, hasLocation={}",
                conversationId, hasCoordinate(longitude, latitude));

        FoodManus foodManus = new FoodManus(foodTools, chatModel, context, longitude, latitude);
        return foodManus.runStream(message);
    }

    private String normalizeConversationId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? "default" : chatId;
    }

    private String resolveUserId(String conversationId) {
        return conversationId;
    }

    private void rememberUserMessage(String conversationId, String message) {
        memoryManager.addUserMessage(conversationId, resolveUserId(conversationId), new UserMessage(message));
    }

    private boolean hasCoordinate(String longitude, String latitude) {
        return longitude != null && !longitude.isBlank()
                && latitude != null && !latitude.isBlank();
    }
}
