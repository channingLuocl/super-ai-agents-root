package com.example.superaiagents.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 附近餐厅推荐工具。
 */
@Slf4j
public class NearbyRestaurantTool {

    private static final int MAX_RADIUS_METERS = 10_000;
    private static final int MIN_RADIUS_METERS = 200;
    private static final int MAX_LIMIT = 8;
    private static final int MAX_ROUTE_LOOKUPS = 5;
    private static final Pattern BUDGET_NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    private final AmapClient amapClient;
    private final int defaultRadiusMeters;
    private final int defaultLimit;

    public NearbyRestaurantTool(AmapClient amapClient, int defaultRadiusMeters, int defaultLimit) {
        this.amapClient = amapClient;
        this.defaultRadiusMeters = defaultRadiusMeters;
        this.defaultLimit = defaultLimit;
    }

    @Tool(description = "Recommend nearby restaurants with rating, cost, route and business info. Use only when the user provides longitude and latitude.")
    public String recommendNearbyRestaurants(
            @ToolParam(description = "User longitude, for example 121.4737") String longitude,
            @ToolParam(description = "User latitude, for example 31.2304") String latitude,
            @ToolParam(required = false, description = "Restaurant keyword or cuisine, for example 火锅、川菜、烧烤、日料. Use 餐厅 when empty.") String keyword,
            @ToolParam(required = false, description = "Search radius in meters. Default 3000, max 10000.") Integer radiusMeters,
            @ToolParam(required = false, description = "Budget preference, for example 人均50-100 or 100左右. Optional.") String budget,
            @ToolParam(required = false, description = "Number of diners. Optional.") Integer peopleCount,
            @ToolParam(required = false, description = "Route mode. Use walking by default; driving is also supported.") String routeMode,
            @ToolParam(required = false, description = "Number of restaurants to return. Default 5, max 8.") Integer limit) {
        log.info("工具调用开始[NearbyRestaurantTool]: longitude={}, latitude={}, keyword={}, radiusMeters={}, budget={}, peopleCount={}, routeMode={}, limit={}",
                longitude, latitude, keyword, radiusMeters, budget, peopleCount, routeMode, limit);
        if (!hasValidCoordinate(longitude, latitude)) {
            return error("缺少有效经纬度，无法推荐附近餐厅。请让用户开启定位或提供当前位置。");
        }
        if (!amapClient.isConfigured()) {
            return error("amap.api-key is not configured. Please set amap.api-key before using restaurant recommendations.");
        }

        int safeRadius = clampRadius(radiusMeters);
        int safeLimit = clampLimit(limit);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedRouteMode = normalizeRouteMode(routeMode);

        try {
            Optional<AmapClient.UserLocationInfo> userLocation = amapClient.reverseGeocode(longitude, latitude);
            int searchSize = Math.min(20, Math.max(safeLimit * 2, 10));
            List<AmapClient.RestaurantPoi> candidates = amapClient
                    .searchRestaurantsAround(longitude, latitude, normalizedKeyword, safeRadius, searchSize)
                    .stream()
                    .sorted(Comparator.comparingDouble(poi -> -scoreRestaurant(poi, budget)))
                    .limit(safeLimit)
                    .toList();

            if (candidates.isEmpty()) {
                JSONObject result = baseResult(normalizedKeyword, safeRadius, safeLimit, budget, peopleCount, normalizedRouteMode, userLocation);
                result.set("message", "附近没有找到匹配的餐厅，可以扩大搜索范围或换一个菜系关键词。");
                result.set("restaurants", new JSONArray());
                log.info("工具调用结束[NearbyRestaurantTool]: keyword={}, restaurants=0", normalizedKeyword);
                return result.toString();
            }

            JSONArray restaurants = new JSONArray();
            for (int i = 0; i < candidates.size(); i++) {
                AmapClient.RestaurantPoi poi = candidates.get(i);
                Optional<AmapClient.RouteInfo> route = i < MAX_ROUTE_LOOKUPS
                        ? amapClient.getRoute(longitude, latitude, poi.entranceLocation(), normalizedRouteMode)
                        : Optional.empty();
                restaurants.add(toRestaurantJson(poi, route));
            }

            JSONObject result = baseResult(normalizedKeyword, safeRadius, safeLimit, budget, peopleCount, normalizedRouteMode, userLocation);
            result.set("message", "success");
            result.set("restaurants", restaurants);
            log.info("工具调用结束[NearbyRestaurantTool]: keyword={}, restaurants={}, names={}",
                    normalizedKeyword, restaurants.size(), summarizeRestaurantNames(restaurants));
            return result.toString();
        } catch (RuntimeException ex) {
            log.warn("工具调用失败[NearbyRestaurantTool]: {}", ex.getMessage());
            return error("餐厅推荐失败：" + ex.getMessage());
        }
    }

    private String summarizeRestaurantNames(JSONArray restaurants) {
        return restaurants.stream()
                .limit(3)
                .map(item -> ((JSONObject) item).getStr("name"))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private JSONObject baseResult(
            String keyword,
            int radiusMeters,
            int limit,
            String budget,
            Integer peopleCount,
            String routeMode,
            Optional<AmapClient.UserLocationInfo> userLocation) {
        JSONObject result = new JSONObject();
        result.set("status", "ok");
        result.set("keyword", keyword);
        result.set("radiusMeters", radiusMeters);
        result.set("limit", limit);
        result.set("budget", StrUtil.blankToDefault(budget, ""));
        result.set("peopleCount", peopleCount);
        result.set("routeMode", routeMode);
        userLocation.ifPresent(location -> result.set("userLocation", toLocationJson(location)));
        return result;
    }

    private JSONObject toLocationJson(AmapClient.UserLocationInfo location) {
        JSONObject json = new JSONObject();
        json.set("formattedAddress", location.formattedAddress());
        json.set("province", location.province());
        json.set("district", location.district());
        json.set("township", location.township());
        json.set("street", location.street());
        json.set("streetNumber", location.streetNumber());
        json.set("businessArea", location.businessArea());
        return json;
    }

    private JSONObject toRestaurantJson(AmapClient.RestaurantPoi poi, Optional<AmapClient.RouteInfo> route) {
        JSONObject restaurant = new JSONObject();
        restaurant.set("name", poi.name());
        restaurant.set("type", poi.type());
        restaurant.set("address", poi.address());
        restaurant.set("distanceMeters", poi.distanceMeters());
        restaurant.set("rating", StrUtil.blankToDefault(poi.rating(), "暂无评分"));
        restaurant.set("cost", StrUtil.blankToDefault(poi.cost(), "暂无人均"));
        restaurant.set("openTimeToday", StrUtil.blankToDefault(poi.openTimeToday(), "暂无营业时间"));
        restaurant.set("tags", poi.tags());
        restaurant.set("businessArea", poi.businessArea());
        restaurant.set("phone", poi.phone());
        restaurant.set("routeDistanceMeters", route.map(AmapClient.RouteInfo::distanceMeters).orElse(poi.distanceMeters()));
        restaurant.set("routeDurationSeconds", route.map(AmapClient.RouteInfo::durationSeconds).orElse(null));
        restaurant.set("photoUrl", poi.photoUrl());
        return restaurant;
    }

    private String error(String message) {
        JSONObject result = new JSONObject();
        result.set("status", "error");
        result.set("message", message);
        result.set("restaurants", new JSONArray());
        return result.toString();
    }

    private boolean hasValidCoordinate(String longitude, String latitude) {
        Double lng = parseDouble(longitude);
        Double lat = parseDouble(latitude);
        return lng != null && lat != null
                && lng >= -180 && lng <= 180
                && lat >= -90 && lat <= 90;
    }

    private int clampRadius(Integer radiusMeters) {
        int radius = radiusMeters == null || radiusMeters <= 0 ? defaultRadiusMeters : radiusMeters;
        return Math.max(MIN_RADIUS_METERS, Math.min(MAX_RADIUS_METERS, radius));
    }

    private int clampLimit(Integer limit) {
        int value = limit == null || limit <= 0 ? defaultLimit : limit;
        return Math.max(1, Math.min(MAX_LIMIT, value));
    }

    private String normalizeRouteMode(String routeMode) {
        String normalized = StrUtil.blankToDefault(routeMode, "walking").trim().toLowerCase();
        return "driving".equals(normalized) ? "driving" : "walking";
    }

    private String normalizeKeyword(String keyword) {
        String normalized = StrUtil.blankToDefault(keyword, "餐厅").trim();
        if (normalized.contains("辣") && !normalized.contains("川菜") && !normalized.contains("火锅")) {
            return "川菜";
        }
        if (normalized.contains("日本") || normalized.contains("寿司")) {
            return "日料";
        }
        if (normalized.contains("烤串")) {
            return "烧烤";
        }
        return normalized;
    }

    private double scoreRestaurant(AmapClient.RestaurantPoi poi, String budget) {
        double score = 0;
        Double rating = parseDouble(poi.rating());
        if (rating != null) {
            score += rating * 10;
        }
        Integer distance = poi.distanceMeters();
        if (distance != null) {
            score += Math.max(0, 10 - distance / 500.0);
        }
        score += budgetScore(poi.cost(), budget);
        return score;
    }

    private double budgetScore(String cost, String budget) {
        Double costValue = parseDouble(cost);
        if (costValue == null || StrUtil.isBlank(budget)) {
            return 0;
        }
        Matcher matcher = BUDGET_NUMBER_PATTERN.matcher(budget);
        Double first = null;
        Double second = null;
        if (matcher.find()) {
            first = Double.parseDouble(matcher.group());
        }
        if (matcher.find()) {
            second = Double.parseDouble(matcher.group());
        }
        if (first == null) {
            return 0;
        }
        if (second != null) {
            double min = Math.min(first, second);
            double max = Math.max(first, second);
            return costValue >= min && costValue <= max ? 12 : -Math.min(8, Math.abs(costValue - max) / 20);
        }
        return costValue <= first ? 8 : -Math.min(8, (costValue - first) / 20);
    }

    private Double parseDouble(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Matcher matcher = BUDGET_NUMBER_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
