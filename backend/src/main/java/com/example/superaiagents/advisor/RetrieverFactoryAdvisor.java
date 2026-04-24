package com.example.superaiagents.advisor;

import com.example.superaiagents.rag.ContextualQueryAugmenterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
@Slf4j
public class RetrieverFactoryAdvisor {
    public static Advisor createFoodAppRagCustomAdvisor(VectorStore vectorStore, double similarityThreshold, int topK, QueryTransformer queryTransformer) {
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(ContextualQueryAugmenterFactory.createInstance())
                .queryTransformers(queryTransformer)
                .build();
    }
}
