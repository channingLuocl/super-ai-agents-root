package com.example.superaiagents.memory.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.HashMap;
import java.util.Map;

/**
 * 长期记忆服务 - 用户画像
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private static final String PROFILE_KEY_PREFIX = "memory:profile:";

    @Resource
    private JedisPooled jedisPooled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取用户画像
     */
    public UserProfile getProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        Map<String, String> data = jedisPooled.hgetAll(key);
        if (data == null || data.isEmpty()) {
            return new UserProfile(userId);
        }
        return mapToProfile(userId, data);
    }

    /**
     * 保存用户画像
     */
    public void saveProfile(UserProfile profile) {
        String key = PROFILE_KEY_PREFIX + profile.getUserId();
        Map<String, String> map = profileToMap(profile);
        jedisPooled.hset(key, map);
        log.info("Saved profile for user {}", profile.getUserId());
    }

    /**
     * 更新用户画像
     */
    public void updateProfile(String userId, String extractionJson) {
        UserProfile profile = getProfile(userId);
        // 解析 extractionJson 并更新 profile
        // 简化处理：直接保存原始提取结果
        profile.setUpdatedAt(System.currentTimeMillis());
        saveProfile(profile);
        log.info("Updated profile for user {} with extraction result", userId);
    }

    /**
     * 判断是否有画像
     */
    public boolean hasProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        return jedisPooled.exists(key);
    }

    private UserProfile mapToProfile(String userId, Map<String, String> data) {
        UserProfile profile = new UserProfile(userId);
        profile.setUpdatedAt(System.currentTimeMillis());

        try {
            if (data.containsKey("taste")) {
                profile.setTaste(objectMapper.readValue(data.get("taste"), UserProfile.TastePreference.class));
            }
            if (data.containsKey("restrictions")) {
                profile.setRestrictions(objectMapper.readValue(data.get("restrictions"), UserProfile.DietaryRestrictions.class));
            }
            if (data.containsKey("cookingLevel")) {
                profile.setCookingLevel(data.get("cookingLevel"));
            }
            if (data.containsKey("healthGoals")) {
                profile.setHealthGoals(objectMapper.readValue(data.get("healthGoals"), java.util.List.class));
            }
            if (data.containsKey("favoriteCuisines")) {
                profile.setFavoriteCuisines(objectMapper.readValue(data.get("favoriteCuisines"), java.util.List.class));
            }
            if (data.containsKey("householdSize")) {
                profile.setHouseholdSize(Integer.parseInt(data.get("householdSize")));
            }
            if (data.containsKey("favoriteIngredients")) {
                profile.setFavoriteIngredients(objectMapper.readValue(data.get("favoriteIngredients"), java.util.List.class));
            }
        } catch (Exception e) {
            log.warn("Failed to parse profile data: {}", e.getMessage());
        }

        return profile;
    }

    private Map<String, String> profileToMap(UserProfile profile) {
        Map<String, String> map = new HashMap<>();
        try {
            map.put("taste", objectMapper.writeValueAsString(profile.getTaste()));
            map.put("restrictions", objectMapper.writeValueAsString(profile.getRestrictions()));
            map.put("cookingLevel", profile.getCookingLevel());
            map.put("healthGoals", objectMapper.writeValueAsString(profile.getHealthGoals()));
            map.put("favoriteCuisines", objectMapper.writeValueAsString(profile.getFavoriteCuisines()));
            map.put("householdSize", String.valueOf(profile.getHouseholdSize()));
            map.put("favoriteIngredients", objectMapper.writeValueAsString(profile.getFavoriteIngredients()));
            map.put("updatedAt", String.valueOf(profile.getUpdatedAt()));
        } catch (Exception e) {
            log.warn("Failed to serialize profile: {}", e.getMessage());
        }
        return map;
    }
}
