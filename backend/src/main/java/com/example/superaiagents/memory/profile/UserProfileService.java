package com.example.superaiagents.memory.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 长期记忆服务 - 用户画像 JSON 文件存储
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final Path profilesDir;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public UserProfileService() {
        this.profilesDir = Path.of(System.getProperty("user.dir"), "memory", "profiles");
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        createDirectories();
    }

    /**
     * 获取用户画像
     */
    public UserProfile getProfile(String userId) {
        Object lock = lockFor(userId);
        synchronized (lock) {
            Path file = profileFile(userId);
            if (!Files.exists(file)) {
                return new UserProfile(userId);
            }
            try {
                UserProfile profile = objectMapper.readValue(file.toFile(), UserProfile.class);
                if (profile.getUserId() == null || profile.getUserId().isBlank()) {
                    profile.setUserId(userId);
                }
                ensureDefaults(profile);
                return profile;
            } catch (Exception e) {
                log.warn("Failed to load user profile JSON, creating new profile: {}", e.getMessage());
                return new UserProfile(userId);
            }
        }
    }

    /**
     * 保存用户画像
     */
    public void saveProfile(UserProfile profile) {
        Object lock = lockFor(profile.getUserId());
        synchronized (lock) {
            try {
                ensureDefaults(profile);
                profile.setUpdatedAt(System.currentTimeMillis());
                writeJsonAtomically(profileFile(profile.getUserId()), profile);
                log.info("Saved profile for user {}", profile.getUserId());
            } catch (Exception e) {
                log.error("Failed to save user profile: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 更新用户画像
     */
    public void updateProfile(String userId, String extractionJson) {
        if (extractionJson == null || extractionJson.isBlank()) {
            return;
        }
        Object lock = lockFor(userId);
        synchronized (lock) {
            UserProfile profile = getProfile(userId);
            try {
                UserProfile extracted = objectMapper.readValue(extractionJson, UserProfile.class);
                mergeProfile(profile, extracted);
                profile.setUpdatedAt(System.currentTimeMillis());
                writeJsonAtomically(profileFile(userId), profile);
                log.info("Updated profile for user {} with extraction result", userId);
            } catch (Exception e) {
                log.warn("Failed to merge extracted profile for user {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 判断是否有画像
     */
    public boolean hasProfile(String userId) {
        return Files.exists(profileFile(userId));
    }

    /**
     * 删除用户画像文件
     */
    public void deleteProfile(String userId) {
        Object lock = lockFor(userId);
        synchronized (lock) {
            try {
                Files.deleteIfExists(profileFile(userId));
                log.info("Deleted profile for user {}", userId);
            } catch (IOException e) {
                log.error("Failed to delete user profile: {}", e.getMessage(), e);
            }
        }
    }

    private void mergeProfile(UserProfile target, UserProfile extracted) {
        ensureDefaults(target);
        ensureDefaults(extracted);

        mergeList(target.getTaste().getPreferred(), extracted.getTaste().getPreferred());
        mergeList(target.getTaste().getDisliked(), extracted.getTaste().getDisliked());

        target.getRestrictions().setVegetarian(
                target.getRestrictions().isVegetarian() || extracted.getRestrictions().isVegetarian());
        target.getRestrictions().setVegan(
                target.getRestrictions().isVegan() || extracted.getRestrictions().isVegan());
        mergeList(target.getRestrictions().getAllergies(), extracted.getRestrictions().getAllergies());
        mergeList(target.getRestrictions().getAvoidIngredients(), extracted.getRestrictions().getAvoidIngredients());

        if (hasText(extracted.getCookingLevel())) {
            target.setCookingLevel(extracted.getCookingLevel());
        }
        mergeList(target.getHealthGoals(), extracted.getHealthGoals());
        mergeList(target.getFavoriteCuisines(), extracted.getFavoriteCuisines());
        mergeList(target.getFavoriteIngredients(), extracted.getFavoriteIngredients());
        if (extracted.getHouseholdSize() > 0) {
            target.setHouseholdSize(extracted.getHouseholdSize());
        }
    }

    private void mergeList(List<String> target, List<String> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        Set<String> values = new LinkedHashSet<>(target);
        for (String item : source) {
            if (hasText(item)) {
                values.add(item.trim());
            }
        }
        target.clear();
        target.addAll(values);
    }

    private void ensureDefaults(UserProfile profile) {
        if (profile.getTaste() == null) {
            profile.setTaste(new UserProfile.TastePreference());
        }
        if (profile.getRestrictions() == null) {
            profile.setRestrictions(new UserProfile.DietaryRestrictions());
        }
        if (profile.getHealthGoals() == null) {
            profile.setHealthGoals(new java.util.ArrayList<>());
        }
        if (profile.getFavoriteCuisines() == null) {
            profile.setFavoriteCuisines(new java.util.ArrayList<>());
        }
        if (profile.getFavoriteIngredients() == null) {
            profile.setFavoriteIngredients(new java.util.ArrayList<>());
        }
        if (!hasText(profile.getCookingLevel())) {
            profile.setCookingLevel("intermediate");
        }
        if (profile.getHouseholdSize() <= 0) {
            profile.setHouseholdSize(2);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void writeJsonAtomically(Path target, Object value) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        objectMapper.writeValue(temp.toFile(), value);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(profilesDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create profile memory directory", e);
        }
    }

    private Path profileFile(String userId) {
        return profilesDir.resolve(safeFileName(userId) + ".json");
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Object lockFor(String key) {
        return locks.computeIfAbsent(safeFileName(key), ignored -> new Object());
    }
}
