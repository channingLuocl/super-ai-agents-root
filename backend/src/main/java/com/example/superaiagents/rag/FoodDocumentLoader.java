package com.example.superaiagents.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 美食应用文档加载器
 */
@Component
public class FoodDocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(FoodDocumentLoader.class);
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("(?m)^\\s*---\\s*$");
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\(([^)]+)\\)");

    private final ResourcePatternResolver resourcePatternResolver;

    public FoodDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档
     * <p>
     * 文档准备和文档读取
     * </p>
     *
     * @return
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
//        加载多篇文档
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");  // classpath:document/*.md 表示：在所有类路径下，查找 document 文件夹中的所有 .md 文件。
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
                List<Document> documents = splitMarkdown(markdown).stream()
                        .map(text -> new Document(text, Map.of("filename", filename)))
                        .map(document -> enrichDocument(document, filename))
                        .toList();
                allDocuments.addAll(documents);
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    private List<String> splitMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        String[] blocks = HORIZONTAL_RULE_PATTERN.split(markdown);
        List<String> documents = new ArrayList<>();
        for (String block : blocks) {
            String trimmed = block.trim();
            if (!trimmed.isBlank()) {
                documents.add(trimmed);
            }
        }
        return documents;
    }

    private Document enrichDocument(Document document, String filename) {
        String text = document.getText();
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("filename", filename);
        metadata.put("title", extractTitle(text));
        metadata.put("source", resolveSource(filename, text));
        metadata.put("sourceUrl", extractField(text, "来源链接"));
        metadata.put("license", extractField(text, "许可证"));
        metadata.put("category", extractField(text, "分类"));
        metadata.put("originalPath", extractField(text, "原始路径"));
        List<String> imageUrls = extractImageUrls(text);
        metadata.put("imageUrls", imageUrls);
        metadata.put("hasImages", String.valueOf(!imageUrls.isEmpty()));
        return document.mutate()
                .metadata(metadata)
                .build();
    }

    private String extractTitle(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                return trimmed.replaceFirst("^#+\\s*", "").trim();
            }
        }
        return "";
    }

    private String resolveSource(String filename, String text) {
        String source = extractField(text, "来源");
        if (source.contains("HowToCook")) {
            return "HowToCook";
        }
        if (source.contains("CookLikeHOC")) {
            return "CookLikeHOC";
        }
        if (filename != null && filename.startsWith("howtocook")) {
            return "HowToCook";
        }
        if (filename != null && filename.startsWith("cooklikehoc")) {
            return "CookLikeHOC";
        }
        return "LocalFAQ";
    }

    private String extractField(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String prefix = "- " + fieldName + "：";
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private List<String> extractImageUrls(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> imageUrls = new ArrayList<>();
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            if (!url.isBlank()) {
                imageUrls.add(url);
            }
        }
        return imageUrls;
    }
}
