package com.example.superaiagents.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 高德 Web 服务 API 客户端。
 */
public class AmapClient {

    private static final String PLACE_AROUND_URL = "https://restapi.amap.com/v5/place/around";
    private static final String WALKING_ROUTE_URL = "https://restapi.amap.com/v3/direction/walking";
    private static final String DRIVING_ROUTE_URL = "https://restapi.amap.com/v3/direction/driving";
    private static final String REVERSE_GEOCODE_URL = "https://restapi.amap.com/v3/geocode/regeo";
    private static final String FOOD_TYPE_CODE = "050000";

    private final String apiKey;

    public AmapClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return StrUtil.isNotBlank(apiKey);
    }

    public List<RestaurantPoi> searchRestaurantsAround(
            String longitude,
            String latitude,
            String keyword,
            int radiusMeters,
            int pageSize) {
        ensureConfigured();
        Map<String, Object> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("location", longitude + "," + latitude);
        params.put("types", FOOD_TYPE_CODE);
        params.put("keywords", StrUtil.blankToDefault(keyword, "餐厅"));
        params.put("radius", radiusMeters);
        params.put("sortrule", "distance");
        params.put("show_fields", "business,navi,photos");
        params.put("page_size", pageSize);
        params.put("page_num", 1);
        params.put("output", "json");

        String response = HttpUtil.get(PLACE_AROUND_URL, params);
        JSONObject json = JSONUtil.parseObj(response);
        assertSuccess(json, "高德周边餐厅搜索失败");

        JSONArray pois = json.getJSONArray("pois");
        if (pois == null || pois.isEmpty()) {
            return List.of();
        }
        return pois.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .map(this::toRestaurantPoi)
                .filter(poi -> StrUtil.isNotBlank(poi.name()))
                .filter(poi -> StrUtil.isBlank(poi.type()) || poi.type().contains("餐饮"))
                .toList();
    }

    public Optional<RouteInfo> getRoute(
            String originLongitude,
            String originLatitude,
            String destinationLocation,
            String routeMode) {
        if (StrUtil.isBlank(destinationLocation)) {
            return Optional.empty();
        }
        try {
            ensureConfigured();
            String normalizedRouteMode = StrUtil.blankToDefault(routeMode, "walking").trim().toLowerCase();
            String routeUrl = "driving".equals(normalizedRouteMode) ? DRIVING_ROUTE_URL : WALKING_ROUTE_URL;
            Map<String, Object> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("origin", originLongitude + "," + originLatitude);
            params.put("destination", destinationLocation);
            params.put("output", "json");
            if ("driving".equals(normalizedRouteMode)) {
                params.put("strategy", 10);
                params.put("extensions", "base");
            }

            String response = HttpUtil.get(routeUrl, params);
            JSONObject json = JSONUtil.parseObj(response);
            assertSuccess(json, "高德路线规划失败");
            JSONObject route = json.getJSONObject("route");
            if (route == null) {
                return Optional.empty();
            }
            JSONArray paths = route.getJSONArray("paths");
            if (paths == null || paths.isEmpty() || !(paths.get(0) instanceof JSONObject path)) {
                return Optional.empty();
            }
            return Optional.of(new RouteInfo(
                    parseInt(path.getStr("distance")),
                    parseInt(path.getStr("duration"))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public Optional<UserLocationInfo> reverseGeocode(String longitude, String latitude) {
        try {
            ensureConfigured();
            Map<String, Object> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("location", longitude + "," + latitude);
            params.put("extensions", "all");
            params.put("output", "json");

            String response = HttpUtil.get(REVERSE_GEOCODE_URL, params);
            JSONObject json = JSONUtil.parseObj(response);
            assertSuccess(json, "高德逆地理编码失败");

            JSONObject regeocode = json.getJSONObject("regeocode");
            if (regeocode == null) {
                return Optional.empty();
            }
            JSONObject addressComponent = regeocode.getJSONObject("addressComponent");
            JSONObject streetNumber = addressComponent != null ? addressComponent.getJSONObject("streetNumber") : null;
            JSONArray businessAreas = addressComponent != null ? addressComponent.getJSONArray("businessAreas") : null;
            String businessArea = "";
            if (businessAreas != null && !businessAreas.isEmpty() && businessAreas.get(0) instanceof JSONObject area) {
                businessArea = cleanText(area.getStr("name"));
            }

            return Optional.of(new UserLocationInfo(
                    cleanText(regeocode.getStr("formatted_address")),
                    addressComponent != null ? cleanText(addressComponent.getStr("province")) : "",
                    addressComponent != null ? cleanText(addressComponent.getStr("district")) : "",
                    addressComponent != null ? cleanText(addressComponent.getStr("township")) : "",
                    streetNumber != null ? cleanText(streetNumber.getStr("street")) : "",
                    streetNumber != null ? cleanText(streetNumber.getStr("number")) : "",
                    businessArea
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("amap.api-key is not configured");
        }
    }

    private void assertSuccess(JSONObject json, String prefix) {
        if (!"1".equals(json.getStr("status"))) {
            String info = StrUtil.blankToDefault(json.getStr("info"), "unknown error");
            String infocode = StrUtil.blankToDefault(json.getStr("infocode"), "unknown infocode");
            throw new IllegalStateException(prefix + ": " + info + " (" + infocode + ")");
        }
    }

    private RestaurantPoi toRestaurantPoi(JSONObject poi) {
        JSONObject business = firstObject(poi, "business", "biz_ext");
        JSONObject navi = poi.getJSONObject("navi");
        JSONArray photos = poi.getJSONArray("photos");
        String photoUrl = "";
        if (photos != null && !photos.isEmpty() && photos.get(0) instanceof JSONObject photo) {
            photoUrl = cleanText(photo.getStr("url"));
        }

        String location = cleanText(poi.getStr("location"));
        String entranceLocation = navi != null ? cleanText(navi.getStr("entr_location")) : "";
        if (StrUtil.isBlank(entranceLocation)) {
            entranceLocation = cleanText(poi.getStr("entr_location"));
        }

        return new RestaurantPoi(
                cleanText(poi.getStr("id")),
                cleanText(poi.getStr("name")),
                cleanText(poi.getStr("type")),
                stringify(poi.get("address")),
                location,
                StrUtil.blankToDefault(entranceLocation, location),
                parseInt(poi.getStr("distance")),
                business != null ? cleanText(business.getStr("rating")) : "",
                business != null ? cleanText(business.getStr("cost")) : "",
                business != null ? cleanText(business.getStr("opentime_today")) : "",
                business != null ? cleanText(business.getStr("opentime_week")) : "",
                business != null ? cleanText(business.getStr("tel")) : cleanText(poi.getStr("tel")),
                business != null ? cleanText(business.getStr("tag")) : "",
                business != null ? cleanText(business.getStr("business_area")) : cleanText(poi.getStr("business_area")),
                photoUrl
        );
    }

    private JSONObject firstObject(JSONObject json, String... keys) {
        for (String key : keys) {
            JSONObject object = json.getJSONObject(key);
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JSONArray array) {
            return array.stream()
                    .map(String::valueOf)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse("");
        }
        return cleanText(String.valueOf(value));
    }

    private String cleanText(String value) {
        if (StrUtil.isBlank(value) || "[]".equals(value)) {
            return "";
        }
        return value.trim();
    }

    private Integer parseInt(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record RestaurantPoi(
            String id,
            String name,
            String type,
            String address,
            String location,
            String entranceLocation,
            Integer distanceMeters,
            String rating,
            String cost,
            String openTimeToday,
            String openTimeWeek,
            String phone,
            String tags,
            String businessArea,
            String photoUrl
    ) {
    }

    public record RouteInfo(Integer distanceMeters, Integer durationSeconds) {
    }

    public record UserLocationInfo(
            String formattedAddress,
            String province,
            String district,
            String township,
            String street,
            String streetNumber,
            String businessArea
    ) {
    }
}
