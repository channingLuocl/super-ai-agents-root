package com.example.superaiagents.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 查询重写器
 * <p>
 * <a href="https://java2ai.com/docs/1.0.0-M6.1/tutorials/rag/#32-query-rewrite-%E6%9F%A5%E8%AF%A2%E9%87%8D%E5%86%99">...</a>
 */
@Component
@Slf4j
public class QueryRewriter {

    private static final String QUERY_REWRITE_SYSTEM_PROMPT = """
            你是 RAG 检索查询改写器。将用户问题改写成适合中文菜谱知识库向量检索的短查询。
            规则：
            1. 只输出一行纯查询文本。
            2. 禁止输出解释、Markdown、标题、编号、英文说明或 <think> 内容。
            3. 如果原问题已经是明确菜名或明确做法，保留核心菜名并补充“做法/步骤”等检索词。
            4. 不要扩展成多段，不要使用冒号说明。
            """;

    private final ChatClient chatClient;

    public QueryRewriter(ChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem(QUERY_REWRITE_SYSTEM_PROMPT)
                .build();
    }

    public String doQueryRewrite(String prompt) {
        String response = chatClient.prompt()
                .user("原始问题：" + prompt)
                .call()
                .content();
        String rewritten = RagQueryTextCleaner.cleanSingleQuery(response, prompt);
        log.info("查询改写: [{}] -> [{}]", prompt, rewritten);
        return rewritten;
    }
}
