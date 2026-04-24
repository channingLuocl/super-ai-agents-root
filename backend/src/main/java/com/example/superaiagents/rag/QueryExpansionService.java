package com.example.superaiagents.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询扩展服务
 * 支持三种扩展模式提升 RAG 检索召回率
 */
@Service
@Slf4j
public class QueryExpansionService {

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 扩展模式
     */
    public enum ExpansionMode {
        /** 规则扩展：同义词词典 */
        RULE,
        /** LLM 扩展：模型生成多表述 */
        LLM,
        /** PRF 扩展：伪相关反馈 */
        PRF
    }

    // 同义词词典（可按需扩展）
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "红烧肉", List.of("东坡肉", "五花肉", "酱肉", "红烧肉类"),
            "鱼香肉丝", List.of("鱼香肉丝做法", "川菜鱼香肉丝"),
            "宫保鸡丁", List.of("宫保鸡丁做法", "川菜宫保鸡丁"),
            "糖醋里脊", List.of("糖醋里脊做法", "酸甜口味"),
            "麻婆豆腐", List.of("麻婆豆腐做法", "川菜麻婆豆腐"),
            "水煮鱼", List.of("水煮鱼做法", "麻辣水煮"),
            "火锅", List.of("火锅做法", "涮锅", "麻辣火锅"),
            "烧烤", List.of("烤肉", "烤串", "BBQ"),
            "沙拉", List.of("凉拌菜", "沙拉做法", "凉菜"),
            "汤", List.of("汤类", "煲汤", "汤品")
    );

    /**
     * 主方法：根据模式扩展查询
     */
    public String expand(String query, ExpansionMode mode) {
        String expanded = switch (mode) {
            case RULE -> expandByRule(query);
            case LLM -> expandByLLM(query);
            case PRF -> expandByPRF(query, null);
        };
        return expanded;
    }

    /**
     * RAG 检索专用的保守扩展。
     * 明确菜名或短查询直接检索，泛化需求才调用 LLM 扩展，避免具体菜名被改写偏移。
     */
    public String expandForRag(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        if (!shouldUseLlmExpansion(query)) {
            log.info("查询扩展: [{}] 为明确菜名，跳过", query);
            return query;
        }
        String expanded = expand(query, ExpansionMode.LLM);
        log.info("查询扩展: [{}] -> [{}]", query, expanded);
        return expanded;
    }

    private boolean shouldUseLlmExpansion(String query) {
        String normalized = query.trim();
        List<String> broadIntentKeywords = List.of(
                "推荐", "适合", "有什么", "哪些", "吃什么", "怎么搭配",
                "早餐", "午餐", "晚餐", "夜宵", "减脂", "高蛋白", "简单", "快手"
        );
        // 含泛化意图词则扩展，优先级高于长度判断
        if (broadIntentKeywords.stream().anyMatch(normalized::contains)) {
            return true;
        }
        // 无泛化意图的短查询（具体菜名）跳过扩展
        return normalized.length() > 12;
    }

    /**
     * 规则扩展：同义词词典替换
     */
    public String expandByRule(String query) {
        StringBuilder result = new StringBuilder(query);
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (query.contains(entry.getKey())) {
                result.append(" | ");
                result.append(String.join(" | ", entry.getValue()));
            }
        }
        return result.toString();
    }

    /**
     * LLM 扩展：让模型生成多个相似表述
     */
    public String expandByLLM(String query) {
        String prompt = """
                对于以下查询，生成3-5个不同的表述方式，用|分隔保持语义一致：
                原始查询：%s
                扩展查询（用|分隔）：
                """.formatted(query);

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 清理响应，只保留扩展部分
        String expanded = response.replaceAll("扩展查询（用\\|分隔）：", "").trim();
        return query + " | " + expanded;
    }

    /**
     * PRF 扩展：伪相关反馈
     * 先用原始查询检索一轮，从结果中提取高频词作为扩展
     */
    public String expandByPRF(String query, VectorStore vectorStore) {
        if (vectorStore == null) {
            log.warn("VectorStore 为空，PRF 扩展跳过");
            return query;
        }

        // 1. 先用原始查询检索
        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(query);

        // 2. 从结果中提取高频词（简单实现：分词后统计）
        Set<String> expandedTerms = new HashSet<>();
        for (org.springframework.ai.document.Document doc : docs) {
            String text = doc.getText();
            // 简单的词提取（实际可用分词器）
            String[] words = text.split("[，,。.\\s]+");
            for (String word : words) {
                if (word.length() >= 2 && word.length() <= 6) {
                    expandedTerms.add(word);
                }
            }
        }

        // 3. 限制扩展词数量
        List<String> topTerms = new ArrayList<>(expandedTerms);
        if (topTerms.size() > 10) {
            topTerms = topTerms.subList(0, 10);
        }

        String result = query + " | " + String.join(" | ", topTerms);
        log.info("PRF扩展: {} -> {}", query, result);
        return result;
    }
}
