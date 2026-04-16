package com.example.superaiagents.controller;

import com.example.superaiagents.memory.MemoryManager;
import com.example.superaiagents.memory.profile.UserProfile;
import com.example.superaiagents.memory.summary.CompressedMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 记忆管理接口
 */
@RestController
@RequestMapping("/memory")
@Slf4j
public class MemoryController {

    @Resource
    private MemoryManager memoryManager;

    /**
     * 获取用户画像
     */
    @GetMapping("/profile/{userId}")
    public Map<String, Object> getUserProfile(@PathVariable String userId) {
        UserProfile profile = memoryManager.getUserProfile(userId);
        return profileToMap(profile);
    }

    /**
     * 获取会话压缩摘要
     */
    @GetMapping("/summary/{chatId}")
    public Map<String, Object> getSessionSummary(@PathVariable String chatId) {
        CompressedMemory memory = memoryManager.getCompressedMemory(chatId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", memory.getSessionId());
        result.put("chunks", memory.getChunks());
        result.put("chunkCount", memory.getChunks().size());
        result.put("fullSummary", memory.getFullSummary());
        return result;
    }

    /**
     * 手动触发压缩
     */
    @PostMapping("/compress/{chatId}")
    public Map<String, Object> compressSession(@PathVariable String chatId,
                                                 @RequestParam(defaultValue = "default") String userId) {
        memoryManager.compressNow(chatId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "压缩成功");
        result.put("sessionId", chatId);
        return result;
    }

    private Map<String, Object> profileToMap(UserProfile profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", profile.getUserId());
        map.put("taste", profile.getTaste());
        map.put("restrictions", profile.getRestrictions());
        map.put("cookingLevel", profile.getCookingLevel());
        map.put("healthGoals", profile.getHealthGoals());
        map.put("favoriteCuisines", profile.getFavoriteCuisines());
        map.put("householdSize", profile.getHouseholdSize());
        map.put("favoriteIngredients", profile.getFavoriteIngredients());
        map.put("updatedAt", profile.getUpdatedAt());
        return map;
    }
}
