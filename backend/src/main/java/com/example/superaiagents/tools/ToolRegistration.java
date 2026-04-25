package com.example.superaiagents.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 核心：
 * <p>
 * 集中注册，把我们开发好的一堆工具，都注册给spring AI，让它可以用，spring AI是通过 ToolCallback 来用的
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Value("${amap.api-key:}")
    private String amapApiKey;

    @Value("${amap.restaurant.default-radius-meters:3000}")
    private int defaultRestaurantRadiusMeters;

    @Value("${amap.restaurant.default-limit:5}")
    private int defaultRestaurantLimit;

    @Bean
    public ToolCallback[] allTools() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        // Default tools for the food recommendation agent. Keep high-risk tools out of this set.
        return ToolCallbacks.from(
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                pdfGenerationTool,
                terminateTool
        );
    }

    @Bean
    public ToolCallback[] foodTools() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        NearbyRestaurantTool nearbyRestaurantTool = new NearbyRestaurantTool(
                new AmapClient(amapApiKey),
                defaultRestaurantRadiusMeters,
                defaultRestaurantLimit
        );
        return ToolCallbacks.from(
                nearbyRestaurantTool, //推荐附近餐厅
                webSearchTool, //网页搜索
                webScrapingTool //网页抓取
        );
    }
}
