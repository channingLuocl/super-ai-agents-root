package com.example.superaiagents.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WebSearchToolTest {
    @Value("${search-api.api-key}")
    private String searchApikey;

    @Test
    void searchWeb() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApikey);
        String query = "程序员鱼皮编程导航 codefather.cn";
        String result = webSearchTool.searchWeb(query);
        Assertions.assertNotNull(result);
    }
}