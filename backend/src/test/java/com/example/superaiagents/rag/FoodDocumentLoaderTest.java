package com.example.superaiagents.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodDocumentLoaderTest {

    @Test
    void loadMarkdownsShouldSplitAndExtractRagMetadata() {
        String markdown = """
                # 宫保鸡丁

                - 来源：Gar-b-age/CookLikeHOC
                - 来源链接：https://github.com/Gar-b-age/CookLikeHOC/blob/main/炒菜/宫保鸡丁.md
                - 许可证：未声明
                - 分类：炒菜
                - 原始路径：炒菜/宫保鸡丁.md

                ![宫保鸡丁](/rag-images/cooklikehoc/images/宫保鸡丁.png)

                ## 配料
                - 鸡丁

                ---

                # 麻婆豆腐

                - 来源：Anduin2017/HowToCook
                - 来源链接：https://github.com/Anduin2017/HowToCook/blob/master/dishes/meat_dish/麻婆豆腐/麻婆豆腐.md
                - 许可证：Unlicense / Public Domain
                - 分类：荤菜
                - 原始路径：dishes/meat_dish/麻婆豆腐/麻婆豆腐.md

                ## 操作
                - 炒香豆瓣酱
                """;

        FoodDocumentLoader loader = new FoodDocumentLoader(new SingleMarkdownResolver("cooklikehoc-recipes-cn-菜谱.md", markdown));

        List<Document> documents = loader.loadMarkdowns();

        assertEquals(2, documents.size());
        Document first = documents.getFirst();
        assertEquals("宫保鸡丁", first.getMetadata().get("title"));
        assertEquals("CookLikeHOC", first.getMetadata().get("source"));
        assertEquals("炒菜", first.getMetadata().get("category"));
        assertEquals("炒菜/宫保鸡丁.md", first.getMetadata().get("originalPath"));
        assertEquals("true", first.getMetadata().get("hasImages"));
        assertTrue(((List<?>) first.getMetadata().get("imageUrls"))
                .contains("/rag-images/cooklikehoc/images/宫保鸡丁.png"));

        Document second = documents.get(1);
        assertEquals("麻婆豆腐", second.getMetadata().get("title"));
        assertEquals("HowToCook", second.getMetadata().get("source"));
        assertEquals("false", second.getMetadata().get("hasImages"));
    }

    private static class SingleMarkdownResolver implements ResourcePatternResolver {

        private final String filename;
        private final String content;

        private SingleMarkdownResolver(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        @Override
        public Resource[] getResources(String locationPattern) {
            return new Resource[]{new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return filename;
                }
            }};
        }

        @Override
        public Resource getResource(String location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }
}
