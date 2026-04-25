package com.example.superaiagents.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具，根据网址抓取一个网站的内容
 */
@Slf4j
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            log.info("工具调用开始[WebScrapingTool]: url={}", url);
            Document document = Jsoup.connect(url).get();
            log.info("工具调用结束[WebScrapingTool]: url={}, htmlLength={}", url, document.html().length());
            return document.html();
        } catch (Exception e) {
            log.warn("工具调用失败[WebScrapingTool]: url={}, error={}", url, e.getMessage());
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
