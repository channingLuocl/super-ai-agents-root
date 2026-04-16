package com.example.superaiagents.memory;

import com.example.superaiagents.memory.profile.UserProfile;
import com.example.superaiagents.memory.profile.UserProfileService;
import com.example.superaiagents.memory.session.SessionMemoryService;
import com.example.superaiagents.memory.summary.CompressedMemory;
import com.example.superaiagents.memory.summary.DialogueChunk;
import com.example.superaiagents.memory.summary.SummaryService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 记忆管理器 - 协调三层记忆系统
 */
@Component
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    private static final int COMPRESS_THRESHOLD = 10;

    @Resource
    private SessionMemoryService sessionMemoryService;

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private SummaryService summaryService;

    /**
     * 添加用户消息并检查是否需要压缩
     */
    public void addUserMessage(String sessionId, String userId, Message message) {
        sessionMemoryService.addMessages(sessionId, List.of(message));

        // 检查是否达到压缩阈值
        int count = sessionMemoryService.getMessageCount(sessionId);
        if (count >= COMPRESS_THRESHOLD) {
            log.info("Session {} 达到压缩阈值 {}，开始压缩", sessionId, count);
            compressSession(sessionId, userId);
        }
    }

    /**
     * 添加 AI 消息
     */
    public void addAiMessage(String sessionId, Message message) {
        sessionMemoryService.addMessages(sessionId, List.of(message));
    }

    /**
     * 获取当前会话的完整上下文（用于 AI 调用）
     */
    public String getContextForAI(String sessionId, String userId) {
        StringBuilder context = new StringBuilder();

        // 1. 添加用户画像（如果存在）
        if (userProfileService.hasProfile(userId)) {
            UserProfile profile = userProfileService.getProfile(userId);
            context.append("【用户偏好】");
            context.append("口味: ").append(profile.getTaste().getPreferred()).append(", ");
            context.append("忌口: ").append(profile.getTaste().getDisliked()).append(", ");
            if (!profile.getRestrictions().getAllergies().isEmpty()) {
                context.append("过敏: ").append(profile.getRestrictions().getAllergies()).append(", ");
            }
            if (!profile.getRestrictions().getAvoidIngredients().isEmpty()) {
                context.append("避免: ").append(profile.getRestrictions().getAvoidIngredients()).append(", ");
            }
            if (!profile.getFavoriteCuisines().isEmpty()) {
                context.append("喜欢菜系: ").append(profile.getFavoriteCuisines()).append(", ");
            }
            context.append("烹饪水平: ").append(profile.getCookingLevel());
            context.append("\n\n");
        }

        // 2. 添加压缩的历史摘要
        CompressedMemory compressedMemory = sessionMemoryService.loadCompressedMemory(sessionId);
        if (!compressedMemory.getChunks().isEmpty()) {
            context.append("【历史对话摘要】");
            context.append(compressedMemory.getFullSummary());
            context.append("\n\n");
        }

        // 3. 添加当前会话的原始消息（如果有）
        List<Message> currentMessages = sessionMemoryService.getMessages(sessionId);
        if (!currentMessages.isEmpty()) {
            context.append("【当前会话】");
            for (Message msg : currentMessages) {
                String role = msg.getMessageType().name().toLowerCase();
                context.append(role).append(": ").append(msg.getText()).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 压缩会话
     */
    private void compressSession(String sessionId, String userId) {
        try {
            // 1. 获取当前会话消息
            List<Message> messages = sessionMemoryService.getMessages(sessionId);
            if (messages.isEmpty()) {
                return;
            }

            // 2. 调用 AI 生成摘要
            String summary = summaryService.generateSummary(messages);
            log.info("Session {} 生成摘要: {}", sessionId, summary);

            // 3. 创建摘要块
            DialogueChunk chunk = new DialogueChunk();
            chunk.setChunkIndex(sessionMemoryService.loadCompressedMemory(sessionId).getChunks().size() + 1);
            chunk.setSummary(summary);
            chunk.setOriginalMessageCount(messages.size());
            chunk.setTimeRangeStart(System.currentTimeMillis());
            chunk.setTimeRangeEnd(System.currentTimeMillis());

            // 4. 保存压缩摘要到文件
            CompressedMemory compressedMemory = sessionMemoryService.loadCompressedMemory(sessionId);
            compressedMemory.addChunk(chunk);
            sessionMemoryService.persistCompressedMemory(compressedMemory);

            // 5. 清空 Redis 中的原始消息，保留最后1条作为锚点
            sessionMemoryService.clearSessionKeepLast(sessionId, 1);

            // 6. 尝试提取用户偏好
            String preferencesJson = summaryService.extractPreferences(summary);
            if (preferencesJson != null) {
                userProfileService.updateProfile(userId, preferencesJson);
                log.info("从对话中提取用户偏好并更新");
            }

        } catch (Exception e) {
            log.error("压缩会话失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动压缩会话，供管理接口调用
     */
    public void compressNow(String sessionId, String userId) {
        compressSession(sessionId, userId);
    }

    /**
     * 删除一个会话关联的短期、中期和长期记忆
     */
    public void deleteConversationMemory(String sessionId, String userId) {
        sessionMemoryService.deleteSessionFiles(sessionId);
        userProfileService.deleteProfile(userId);
    }

    /**
     * 获取压缩记忆
     */
    public CompressedMemory getCompressedMemory(String sessionId) {
        return sessionMemoryService.loadCompressedMemory(sessionId);
    }

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String userId) {
        return userProfileService.getProfile(userId);
    }
}
