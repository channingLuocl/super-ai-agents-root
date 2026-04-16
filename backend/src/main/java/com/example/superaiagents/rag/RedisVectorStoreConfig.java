package com.example.superaiagents.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * 美食应用 Redis 向量存储配置。
 * 文档加载和增量同步由 {@link RagIndexSynchronizer} 负责。
 */
@Configuration
public class RedisVectorStoreConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Jedis 客户端 Bean，供其他服务使用
     */
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    /**
     * 初始化 Redis 向量库。文档写入由 RagIndexSynchronizer 在应用启动完成后执行。
     */
    @Bean
    public VectorStore redisVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        JedisPooled jedisPooled = jedisPooled();

        return RedisVectorStore.builder(jedisPooled, dashscopeEmbeddingModel)
                .indexName("spring_ai_index")
                .prefix("doc:")
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("source"),
                        RedisVectorStore.MetadataField.tag("category"),
                        RedisVectorStore.MetadataField.tag("hasImages"),
                        RedisVectorStore.MetadataField.text("filename"),
                        RedisVectorStore.MetadataField.text("title"),
                        RedisVectorStore.MetadataField.text("sourceUrl"),
                        RedisVectorStore.MetadataField.text("originalPath")
                )
                .initializeSchema(true)
                .build();
    }
}
