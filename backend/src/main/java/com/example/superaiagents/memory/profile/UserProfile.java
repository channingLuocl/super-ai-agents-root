package com.example.superaiagents.memory.profile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 长期记忆：用户美食偏好画像
 */
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private TastePreference taste;
    private DietaryRestrictions restrictions;
    private String cookingLevel;
    private List<String> healthGoals;
    private List<String> favoriteCuisines;
    private int householdSize;
    private List<String> favoriteIngredients;
    private long updatedAt;

    public UserProfile(String userId) {
        this.userId = userId;
        this.taste = new TastePreference();
        this.restrictions = new DietaryRestrictions();
        this.cookingLevel = "intermediate";
        this.healthGoals = new ArrayList<>();
        this.favoriteCuisines = new ArrayList<>();
        this.householdSize = 2;
        this.favoriteIngredients = new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }

    public UserProfile() {
        this.taste = new TastePreference();
        this.restrictions = new DietaryRestrictions();
        this.healthGoals = new ArrayList<>();
        this.favoriteCuisines = new ArrayList<>();
        this.favoriteIngredients = new ArrayList<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TastePreference getTaste() {
        return taste;
    }

    public void setTaste(TastePreference taste) {
        this.taste = taste;
    }

    public DietaryRestrictions getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(DietaryRestrictions restrictions) {
        this.restrictions = restrictions;
    }

    public String getCookingLevel() {
        return cookingLevel;
    }

    public void setCookingLevel(String cookingLevel) {
        this.cookingLevel = cookingLevel;
    }

    public List<String> getHealthGoals() {
        return healthGoals;
    }

    public void setHealthGoals(List<String> healthGoals) {
        this.healthGoals = healthGoals;
    }

    public List<String> getFavoriteCuisines() {
        return favoriteCuisines;
    }

    public void setFavoriteCuisines(List<String> favoriteCuisines) {
        this.favoriteCuisines = favoriteCuisines;
    }

    public int getHouseholdSize() {
        return householdSize;
    }

    public void setHouseholdSize(int householdSize) {
        this.householdSize = householdSize;
    }

    public List<String> getFavoriteIngredients() {
        return favoriteIngredients;
    }

    public void setFavoriteIngredients(List<String> favoriteIngredients) {
        this.favoriteIngredients = favoriteIngredients;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void updateFromExtraction(String extractionResult) {
        this.updatedAt = System.currentTimeMillis();
    }

    public static class TastePreference implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<String> preferred = new ArrayList<>();
        private List<String> disliked = new ArrayList<>();

        public TastePreference() {
        }

        public List<String> getPreferred() {
            return preferred;
        }

        public void setPreferred(List<String> preferred) {
            this.preferred = preferred;
        }

        public List<String> getDisliked() {
            return disliked;
        }

        public void setDisliked(List<String> disliked) {
            this.disliked = disliked;
        }
    }

    public static class DietaryRestrictions implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean vegetarian;
        private boolean vegan;
        private List<String> allergies = new ArrayList<>();
        private List<String> avoidIngredients = new ArrayList<>();

        public DietaryRestrictions() {
        }

        public boolean isVegetarian() {
            return vegetarian;
        }

        public void setVegetarian(boolean vegetarian) {
            this.vegetarian = vegetarian;
        }

        public boolean isVegan() {
            return vegan;
        }

        public void setVegan(boolean vegan) {
            this.vegan = vegan;
        }

        public List<String> getAllergies() {
            return allergies;
        }

        public void setAllergies(List<String> allergies) {
            this.allergies = allergies;
        }

        public List<String> getAvoidIngredients() {
            return avoidIngredients;
        }

        public void setAvoidIngredients(List<String> avoidIngredients) {
            this.avoidIngredients = avoidIngredients;
        }
    }
}
