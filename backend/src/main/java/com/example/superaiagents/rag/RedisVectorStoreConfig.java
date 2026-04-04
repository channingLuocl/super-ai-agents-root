package com.example.superaiagents.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.JedisPooled;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 美食应用 Redis 向量存储配置（Bean 形式，带去重/更新）
 */
@Configuration
public class RedisVectorStoreConfig {

    @Resource
    private FoodDocumentLoader foodDocumentLoader;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // DashScope Embedding API 批量限制
    private static final int BATCH_SIZE = 10;

    /**
     * 初始化 Redis 向量库并加载文档（带去重、唯一ID、更新逻辑）
     */
    @Bean
    public VectorStore redisVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 1. 创建 Jedis 客户端（连接 Redis）
        JedisPooled jedisPooled = new JedisPooled(redisHost, redisPort);

        // 2. 创建 RedisVectorStore
        RedisVectorStore redisVectorStore = RedisVectorStore.builder(jedisPooled, dashscopeEmbeddingModel)
                .indexName("spring_ai_index")
                .prefix("doc:")
                .initializeSchema(true)
                .build();

        // 3. 加载 Markdown 文档
        List<Document> documentList = foodDocumentLoader.loadMarkdowns();

        if (documentList.isEmpty()) {
            System.out.println("⚠️  未加载到任何 Markdown 文档，跳过向量库添加");
            return redisVectorStore;
        }

        // 4. 生成带前缀的唯一 ID（避免解析冲突）
        List<Document> documentsWithUniqueId = documentList.stream()
                .map(this::generateUniqueIdByText)
                .collect(Collectors.toList());

        // 5. 分批添加文档（DashScope Embedding API 限制每批最多 10 个）
        int totalDocs = documentsWithUniqueId.size();
        for (int i = 0; i < totalDocs; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalDocs);
            List<Document> batch = documentsWithUniqueId.subList(i, end);
            redisVectorStore.add(batch);
            System.out.printf("📄 已添加第 %d-%d 个文档（共 %d 个）%n", i + 1, end, totalDocs);
        }

        System.out.printf("✅ 成功加载 %d 个文档到 Redis 向量库%n", totalDocs);

        return redisVectorStore;
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