package com.example.superaiagents.app;


import com.example.superaiagents.advisor.RetrieverFactoryAdvisor;
import com.example.superaiagents.memory.MemoryManager;
import com.example.superaiagents.pojo.FoodReport;
import com.example.superaiagents.rag.QueryExpansionService;
import com.example.superaiagents.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class FoodApp {

    private static final String SYSTEM_PROMPT_PREFIX = "扮演深耕美食领域的专家\"小迪\"。开场向用户表明身份，告知用户可以询问任何美食相关问题。" +
            "围绕以下几个方面与用户交流：\n" +
            "- 菜谱推荐：根据用户的需求推荐菜品做法\n" +
            "- 食材知识：介绍各种食材的选购、保存和营养知识\n" +
            "- 烹饪技巧：分享烹饪技巧和注意事项\n" +
            "- 美食文化：介绍各地美食特色和饮食文化\n" +
            "- 餐厅推荐：推荐特色餐厅和小吃\n" +
            "引导用户详述需求，比如口味偏好、预算、场合等，以便给出专属的美食建议。\n\n";

    private final ChatClient chatClient;

    // AI 美食知识库问答功能，通过变量名注入
    @Resource(name = "redisVectorStore")
    private VectorStore redisVectorStore;

    //    注入查询重写器
    @Resource
    private QueryRewriter queryRewriter;

    //    注入查询扩展服务
    @Resource
    private QueryExpansionService queryExpansionService;

    //    AI调用工具能力
    @Resource
    private ToolCallback[] allTools;

    // AI 调用 MCP 服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    // 记忆管理器
    @Resource
    private MemoryManager memoryManager;

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public FoodApp(ChatModel dashscopeChatModel) {
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT_PREFIX)
                .build();
    }

    /**
     * 1. AI 基础对话（支持多轮对话记忆 + 三层记忆系统）
     *
     * @param message
     * @param chatId 会话ID，默认 "default"
     * @return
     */
    public String doChat(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("AI 回答的内容是: \n{}", content);

        rememberAiMessage(conversationId, content);

        return content;
    }

    /**
     * 1. AI 基础对话（支持多轮对话记忆 + 三层记忆系统，SSE流式传输）
     *
     * @param message
     * @param chatId 会话ID，默认 "default"
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);

        return streamAndRemember(conversationId, chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content());
    }

    /**
     * 2. AI 美食报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public FoodReport doChatWithReport(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId)
                + "\n每次对话后都要生成美食报告，标题为推荐菜品名称，内容为制作建议列表";
        FoodReport foodReport = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(FoodReport.class);
        log.info("foodReport: {}", foodReport);
        rememberAiMessage(conversationId, foodReport != null ? foodReport.toString() : "");
        return foodReport;
    }

    /**
     * 3. AI 基础对话+查询重写
     *
     * @param message
     * @param chatId
     * @return
     */

    public String doChatRewrite(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入查询重写后回答的内容是: \n{}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }

    /**
     * 4.1 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        // 查询扩展（使用 LLM 模式）
        String expandedMessage = queryExpansionService.expand(message, QueryExpansionService.ExpansionMode.LLM);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(expandedMessage)
                // 先配置参数，再添加Advisor（根据SDK支持的链式调用顺序）
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 直接传入Advisor实例
                .advisors(new QuestionAnswerAdvisor(redisVectorStore))
                // 执行调用
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入rag+查询扩展之后的输出 content内容:\n {}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }

    /**
     * 4.1.1 和 RAG 知识库进行对话（SSE流式）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatWithRagStream(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        // 查询扩展（使用 LLM 模式）
        String expandedMessage = queryExpansionService.expand(message, QueryExpansionService.ExpansionMode.LLM);
        return streamAndRemember(conversationId, chatClient
                .prompt()
                .system(systemPrompt)
                .user(expandedMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(new QuestionAnswerAdvisor(redisVectorStore))
                .stream()
                .content());
    }


    /**
     * 4.2 和 RAG 知识库进行对话，并且在对话之前使用 检索器配置
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRagWithRetriever(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(RetrieverFactoryAdvisor.createFoodAppRagCustomAdvisor(
                        redisVectorStore, "美食"
                ))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入检索器后的 content内容:\n {}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }


    /**
     * 5.1 AI美食功能(工具调用)
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        ChatResponse response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("工具调用的测试 content: \n{}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }

    /**
     * 5.2 AI 美食（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcpTools(String message, String chatId) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildSystemPromptWithMemory(conversationId);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }

    private String normalizeConversationId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? "default" : chatId;
    }

    private String resolveUserId(String conversationId) {
        return conversationId;
    }

    private void rememberUserMessage(String conversationId, String message) {
        memoryManager.addUserMessage(conversationId, resolveUserId(conversationId), new UserMessage(message));
    }

    private void rememberAiMessage(String conversationId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        memoryManager.addAiMessage(conversationId, new AssistantMessage(content));
    }

    private String buildSystemPromptWithMemory(String conversationId) {
        String enhancedContext = memoryManager.getContextForAI(conversationId, resolveUserId(conversationId));
        if (enhancedContext == null || enhancedContext.isBlank()) {
            return SYSTEM_PROMPT_PREFIX;
        }
        return SYSTEM_PROMPT_PREFIX + "【上下文】\n" + enhancedContext;
    }

    private Flux<String> streamAndRemember(String conversationId, Flux<String> contentFlux) {
        StringBuilder assistantContent = new StringBuilder();
        return contentFlux
                .doOnNext(assistantContent::append)
                .doOnComplete(() -> rememberAiMessage(conversationId, assistantContent.toString()))
                .doOnError(error -> log.warn("流式对话未完成，跳过 AI 回复记忆写入: {}", error.getMessage()));
    }

}
