package com.example.superaiagents.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于本地菜谱知识库的做饭推荐工具。
 */
@Slf4j
public class RecipeRecommendTool {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 5;
    private static final int SEARCH_TOP_K = 12;
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TOTAL_MINUTES_PATTERN = Pattern.compile("总计\\s*(\\d+)\\s*分钟");
    private static final Pattern PREP_AND_COOK_PATTERN = Pattern.compile("预计备菜\\s*(\\d+)\\s*分钟，烹饪\\s*(\\d+)\\s*分钟，总计\\s*(\\d+)\\s*分钟");
    private static final Pattern HOUR_RANGE_PATTERN = Pattern.compile("(\\d+)(?:\\s*-\\s*(\\d+))?\\s*小时");
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("预估烹饪难度：([★☆]+)");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^[+-]\\s*(.+)$");

    private final VectorStore vectorStore;

    public RecipeRecommendTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Recommend home-cooking recipes from the local recipe knowledge base")
    public String recommendRecipes(
            @ToolParam(required = false, description = "Recipe keyword, such as 家常菜、快手菜、下饭菜") String keyword,
            @ToolParam(required = false, description = "Available ingredients, such as 鸡蛋,番茄,土豆") String ingredients,
            @ToolParam(required = false, description = "Taste preference, such as 清淡、香辣、酸甜") String taste,
            @ToolParam(required = false, description = "Number of diners") Integer peopleCount,
            @ToolParam(required = false, description = "Maximum cooking time in minutes") Integer maxCookingMinutes,
            @ToolParam(required = false, description = "Difficulty preference, such as 新手、家常、进阶") String difficulty,
            @ToolParam(required = false, description = "Diet goal, such as 减脂、控糖、高蛋白") String dietGoal,
            @ToolParam(required = false, description = "Ingredients to avoid, such as 花生,香菜") String avoidIngredients,
            @ToolParam(required = false, description = "Number of recipes to return. Default 3, max 5.") Integer limit) {
        log.info("工具调用开始[RecipeRecommendTool]: keyword={}, ingredients={}, taste={}, peopleCount={}, maxCookingMinutes={}, difficulty={}, dietGoal={}, avoidIngredients={}, limit={}",
                keyword, ingredients, taste, peopleCount, maxCookingMinutes, difficulty, dietGoal, avoidIngredients, limit);
        String query = buildQuery(keyword, ingredients, taste, peopleCount, maxCookingMinutes, difficulty, dietGoal, avoidIngredients);
        int safeLimit = clampLimit(limit);

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(SEARCH_TOP_K)
                .similarityThresholdAll()
                .build());

        List<String> avoidTerms = splitTerms(avoidIngredients);
        List<String> ingredientTerms = splitTerms(ingredients);
        List<RecipeCandidate> candidates = documents.stream()
                .map(this::toCandidate)
                .filter(candidate -> !candidate.name().isBlank())
                .filter(candidate -> avoidTerms.stream().noneMatch(term -> containsIgnoreCase(candidate.sourceText(), term)))
                .sorted(Comparator.comparingDouble((RecipeCandidate candidate) ->
                        -scoreCandidate(candidate, ingredientTerms, taste, maxCookingMinutes, difficulty, dietGoal)))
                .toList();

        LinkedHashSet<String> seenNames = new LinkedHashSet<>();
        JSONArray recipes = new JSONArray();
        for (RecipeCandidate candidate : candidates) {
            if (recipes.size() >= safeLimit) {
                break;
            }
            if (!seenNames.add(candidate.name())) {
                continue;
            }
            recipes.add(toRecipeJson(candidate, ingredientTerms, taste, peopleCount, maxCookingMinutes, difficulty, dietGoal));
        }

        JSONObject result = new JSONObject();
        result.set("status", "ok");
        result.set("keyword", StrUtil.blankToDefault(keyword, ""));
        result.set("ingredients", StrUtil.blankToDefault(ingredients, ""));
        result.set("taste", StrUtil.blankToDefault(taste, ""));
        result.set("peopleCount", peopleCount);
        result.set("maxCookingMinutes", maxCookingMinutes);
        result.set("difficulty", StrUtil.blankToDefault(difficulty, ""));
        result.set("dietGoal", StrUtil.blankToDefault(dietGoal, ""));
        result.set("avoidIngredients", StrUtil.blankToDefault(avoidIngredients, ""));
        result.set("limit", safeLimit);
        result.set("recipes", recipes);

        if (recipes.isEmpty()) {
            result.set("message", "暂时没有找到足够匹配的菜谱，可以换个菜名、食材或放宽条件再试试。");
        } else {
            result.set("message", "success");
        }
        log.info("工具调用结束[RecipeRecommendTool]: query={}, vectorHits={}, recipes={}, names={}",
                query, documents.size(), recipes.size(), summarizeRecipeNames(recipes));
        return result.toString();
    }

    private String summarizeRecipeNames(JSONArray recipes) {
        return recipes.stream()
                .limit(3)
                .map(item -> ((JSONObject) item).getStr("name"))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String buildQuery(String keyword,
                              String ingredients,
                              String taste,
                              Integer peopleCount,
                              Integer maxCookingMinutes,
                              String difficulty,
                              String dietGoal,
                              String avoidIngredients) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword)) {
            parts.add(keyword.trim());
        }
        if (StrUtil.isNotBlank(ingredients)) {
            parts.add("食材 " + ingredients.trim());
        }
        if (StrUtil.isNotBlank(taste)) {
            parts.add("口味 " + taste.trim());
        }
        if (peopleCount != null && peopleCount > 0) {
            parts.add(peopleCount + "人份");
        }
        if (maxCookingMinutes != null && maxCookingMinutes > 0) {
            parts.add(maxCookingMinutes + "分钟内");
        }
        if (StrUtil.isNotBlank(difficulty)) {
            parts.add(difficulty.trim());
        }
        if (StrUtil.isNotBlank(dietGoal)) {
            parts.add(dietGoal.trim());
        }
        if (StrUtil.isNotBlank(avoidIngredients)) {
            parts.add("不含 " + avoidIngredients.trim());
        }
        if (parts.isEmpty()) {
            parts.add("家常菜 菜谱");
        }
        return String.join(" ", parts);
    }

    private int clampLimit(Integer limit) {
        int value = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        return Math.max(1, Math.min(MAX_LIMIT, value));
    }

    private RecipeCandidate toCandidate(Document document) {
        String text = StrUtil.blankToDefault(document.getText(), "");
        String name = StrUtil.blankToDefault(asString(document.getMetadata().get("title")), extractTitle(text));
        int estimatedMinutes = extractEstimatedMinutes(text);
        String difficulty = extractDifficulty(text);
        List<String> mainIngredients = extractMainIngredients(text);
        String summary = extractSummary(text, name);
        return new RecipeCandidate(name, summary, difficulty, estimatedMinutes, mainIngredients, text);
    }

    private JSONObject toRecipeJson(RecipeCandidate candidate,
                                    List<String> ingredientTerms,
                                    String taste,
                                    Integer peopleCount,
                                    Integer maxCookingMinutes,
                                    String difficulty,
                                    String dietGoal) {
        JSONObject json = new JSONObject();
        json.set("name", candidate.name());
        json.set("reason", buildReason(candidate, ingredientTerms, taste, peopleCount, maxCookingMinutes, difficulty, dietGoal));
        json.set("difficulty", candidate.difficulty());
        json.set("estimatedTimeMinutes", candidate.estimatedMinutes() > 0 ? candidate.estimatedMinutes() : null);
        json.set("mainIngredients", new JSONArray(candidate.mainIngredients()));
        json.set("summary", candidate.summary());
        return json;
    }

    private double scoreCandidate(RecipeCandidate candidate,
                                  List<String> ingredientTerms,
                                  String taste,
                                  Integer maxCookingMinutes,
                                  String difficulty,
                                  String dietGoal) {
        double score = 0;
        for (String term : ingredientTerms) {
            if (containsIgnoreCase(candidate.sourceText(), term)) {
                score += 8;
            }
        }
        if (StrUtil.isNotBlank(taste) && containsIgnoreCase(candidate.sourceText(), taste)) {
            score += 4;
        }
        if (maxCookingMinutes != null && maxCookingMinutes > 0 && candidate.estimatedMinutes() > 0) {
            score += candidate.estimatedMinutes() <= maxCookingMinutes ? 6 : -6;
        }
        if (StrUtil.isNotBlank(difficulty) && matchesDifficulty(candidate.difficulty(), difficulty)) {
            score += 4;
        }
        if (StrUtil.isNotBlank(dietGoal) && containsIgnoreCase(candidate.sourceText(), dietGoal)) {
            score += 3;
        }
        if (candidate.estimatedMinutes() > 0) {
            score += Math.max(0, 4 - candidate.estimatedMinutes() / 20.0);
        }
        return score;
    }

    private String buildReason(RecipeCandidate candidate,
                               List<String> ingredientTerms,
                               String taste,
                               Integer peopleCount,
                               Integer maxCookingMinutes,
                               String difficulty,
                               String dietGoal) {
        List<String> reasons = new ArrayList<>();
        List<String> matchedIngredients = ingredientTerms.stream()
                .filter(term -> containsIgnoreCase(candidate.sourceText(), term))
                .toList();
        if (!matchedIngredients.isEmpty()) {
            reasons.add("食材匹配：" + String.join("、", matchedIngredients));
        }
        if (maxCookingMinutes != null && maxCookingMinutes > 0 && candidate.estimatedMinutes() > 0
                && candidate.estimatedMinutes() <= maxCookingMinutes) {
            reasons.add("适合 " + maxCookingMinutes + " 分钟内完成");
        }
        if (StrUtil.isNotBlank(taste) && containsIgnoreCase(candidate.sourceText(), taste)) {
            reasons.add("口味贴近“" + taste.trim() + "”");
        }
        if (StrUtil.isNotBlank(difficulty) && matchesDifficulty(candidate.difficulty(), difficulty)) {
            reasons.add("难度符合“" + difficulty.trim() + "”");
        }
        if (peopleCount != null && peopleCount > 0) {
            reasons.add("可按 " + peopleCount + " 人份灵活调整");
        }
        if (StrUtil.isNotBlank(dietGoal)) {
            reasons.add("可结合“" + dietGoal.trim() + "”目标做细化调整");
        }
        if (reasons.isEmpty()) {
            reasons.add("和当前需求接近，适合作为本次下厨候选");
        }
        return String.join("；", reasons);
    }

    private List<String> splitTerms(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，、/\\s]+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private String extractTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).replace("的做法", "").trim() : "";
    }

    private int extractEstimatedMinutes(String text) {
        Matcher exactMatcher = PREP_AND_COOK_PATTERN.matcher(text);
        if (exactMatcher.find()) {
            return Integer.parseInt(exactMatcher.group(3));
        }
        Matcher totalMatcher = TOTAL_MINUTES_PATTERN.matcher(text);
        if (totalMatcher.find()) {
            return Integer.parseInt(totalMatcher.group(1));
        }
        Matcher hourMatcher = HOUR_RANGE_PATTERN.matcher(text);
        if (hourMatcher.find()) {
            int first = Integer.parseInt(hourMatcher.group(1));
            String secondRaw = hourMatcher.group(2);
            if (secondRaw != null) {
                int second = Integer.parseInt(secondRaw);
                return ((first + second) / 2) * 60;
            }
            return first * 60;
        }
        return -1;
    }

    private String extractDifficulty(String text) {
        Matcher matcher = DIFFICULTY_PATTERN.matcher(text);
        if (!matcher.find()) {
            return "unknown";
        }
        int stars = (int) matcher.group(1).chars().filter(ch -> ch == '★').count();
        if (stars <= 2) {
            return "easy";
        }
        if (stars == 3) {
            return "medium";
        }
        return "hard";
    }

    private List<String> extractMainIngredients(String text) {
        List<String> ingredients = new ArrayList<>();
        boolean inIngredients = false;
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                inIngredients = trimmed.contains("必备原料和工具");
                continue;
            }
            if (!inIngredients) {
                continue;
            }
            Matcher matcher = BULLET_PATTERN.matcher(trimmed);
            if (!matcher.find()) {
                if (!ingredients.isEmpty()) {
                    break;
                }
                continue;
            }
            String item = matcher.group(1)
                    .replaceAll("（.*?）", "")
                    .replaceAll("\\(.*?\\)", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!item.isBlank()) {
                ingredients.add(item);
            }
            if (ingredients.size() >= 5) {
                break;
            }
        }
        return ingredients;
    }

    private String extractSummary(String text, String title) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("- 来源")
                    || trimmed.startsWith("- 许可证")
                    || trimmed.startsWith("- 分类")
                    || trimmed.startsWith("- 原始路径")
                    || trimmed.startsWith("![")
                    || trimmed.startsWith("预估烹饪难度")
                    || trimmed.startsWith("预计备菜")) {
                continue;
            }
            if (trimmed.contains(title) || trimmed.length() >= 10) {
                return trimmed;
            }
        }
        return title + "，适合作为本次做饭推荐。";
    }

    private boolean matchesDifficulty(String candidateDifficulty, String requestedDifficulty) {
        String normalized = requestedDifficulty.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("新手") || normalized.contains("简单") || normalized.contains("家常")) {
            return "easy".equals(candidateDifficulty) || "medium".equals(candidateDifficulty);
        }
        if (normalized.contains("进阶") || normalized.contains("复杂")) {
            return "hard".equals(candidateDifficulty);
        }
        return normalized.equals(candidateDifficulty);
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (StrUtil.isBlank(source) || StrUtil.isBlank(target)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(target.trim().toLowerCase(Locale.ROOT));
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record RecipeCandidate(
            String name,
            String summary,
            String difficulty,
            int estimatedMinutes,
            List<String> mainIngredients,
            String sourceText
    ) {
    }
}
