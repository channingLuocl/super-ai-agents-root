package com.example.superaiagents.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 恋爱大师 Redis 向量存储配置（Bean 形式，带去重/更新）
 */
@Configuration
public class RedisVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     * 初始化 Redis 向量库并加载文档（带去重、唯一ID、更新逻辑）
     */
    @Bean
    public VectorStore redisVectorStore(EmbeddingModel dashscopeEmbeddingModel, VectorStore vectorStore) {
        // 1. 加载 Markdown 文档
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();

        if (documentList.isEmpty()) {
            System.out.println("⚠️  未加载到任何 Markdown 文档，跳过向量库添加");
            return vectorStore;
        }

        // 2. 生成带前缀的唯一 ID（避免解析冲突）
        List<Document> documentsWithUniqueId = documentList.stream()
                .map(this::generateUniqueIdByText)
                .collect(Collectors.toList());

        // 3. 先删后加：实现去重和文档更新（删除旧版本，添加新版本）
        documentsWithUniqueId.forEach(doc -> {
            try {
                vectorStore.delete(doc.getId());
            } catch (Exception e) {
                System.err.printf("⚠️  删除旧文档 ID [%s] 失败，将直接添加新文档：%s%n",
                        doc.getId(), e.getMessage());
            }
        });

        // 4. 批量添加新文档到向量库
        vectorStore.add(documentsWithUniqueId);
        System.out.printf("✅ 成功加载 %d 个文档到 Redis 向量库（已去重/更新）%n",
                documentsWithUniqueId.size());

        return vectorStore;
    }

    /**
     * 基于文档内容生成唯一 ID（前缀+MD5，确保格式合法且唯一）
     */
    private Document generateUniqueIdByText(Document document) {
        String content = document.getText();
        String uniqueId = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        return document.mutate()
                .id(uniqueId)
                .build();
    }
}