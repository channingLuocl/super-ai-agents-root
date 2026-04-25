package com.example.superaiagents.app;


import com.example.superaiagents.advisor.RetrieverFactoryAdvisor;
import com.example.superaiagents.memory.MemoryManager;
import com.example.superaiagents.pojo.FoodReport;
import com.example.superaiagents.rag.QueryExpansionService;
import com.example.superaiagents.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String RAG_PROMPT_SUFFIX = """

            【知识库回答规则】
            - 优先基于检索到的菜谱、食材知识和烹饪技巧回答。
            - 如果检索内容包含图片链接，可以在回答末尾用“相关图片：”列出链接。
            - 不要编造图片链接、来源链接或不存在于知识库上下文的菜谱细节。
            - 如果知识库没有找到可靠内容，要明确说明没有找到对应菜谱，再给出通用建议。
            """;

    private static final String TOOL_PROMPT_SUFFIX = """

            【工具调用规则】
            - 当用户询问附近、周边、餐厅、出去吃、外面吃、晚上吃什么且明显想找店时，优先调用 recommendNearbyRestaurants。
            - 调用餐厅推荐工具时，必须使用系统消息中提供的 longitude 和 latitude，禁止编造坐标。
            - 如果没有可用坐标，不要调用附近餐厅工具，要请用户开启定位或提供当前位置/商圈。
            - 餐厅推荐结果要控制在 3-5 家，说明距离、评分、人均、营业时间和路线耗时；字段缺失时如实说明。
            - 餐厅推荐信息来自高德地图，评分、人均、营业时间可能为空或不是实时最终状态，回答时保持谨慎。
            - 最终回答禁止输出 <think>、</think> 或任何思考过程标签，只输出给用户看的内容。
            """;

    private final ChatClient chatClient;

    @Value("${rag.similarity-threshold:0.5}")
    private double ragSimilarityThreshold;

    @Value("${rag.top-k:8}")
    private int ragTopK;

    // AI 美食知识库问答功能，通过变量名注入
    @Resource(name = "redisVectorStore")
    private VectorStore redisVectorStore;

    //    注入查询重写器
    @Resource
    private QueryRewriter queryRewriter;

    //    注入查询扩展服务
    @Resource
    private QueryExpansionService queryExpansionService;

    //    AI 美食专用工具能力
    @Resource(name = "foodTools")
    private ToolCallback[] foodTools;

    // AI 调用 MCP 服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    // 记忆管理器
    @Resource
    private MemoryManager memoryManager;

    public FoodApp(ChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel)
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
        String systemPrompt = buildRagSystemPrompt(conversationId);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(RetrieverFactoryAdvisor.createFoodAppRagCustomAdvisor(redisVectorStore, ragSimilarityThreshold, ragTopK, buildRagQueryTransformer()))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入rag+查询改写+查询扩展之后的输出 content内容:\n {}", content);
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
        String systemPrompt = buildRagSystemPrompt(conversationId);
        return streamAndRemember(conversationId, chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(RetrieverFactoryAdvisor.createFoodAppRagCustomAdvisor(redisVectorStore, ragSimilarityThreshold, ragTopK, buildRagQueryTransformer()))
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
                .advisors(RetrieverFactoryAdvisor.createFoodAppRagCustomAdvisor(redisVectorStore, ragSimilarityThreshold, ragTopK, buildRagQueryTransformer()))
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
                .toolCallbacks(foodTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("工具调用的测试 content: \n{}", content);
        rememberAiMessage(conversationId, content);
        return content;
    }

    /**
     * 5.1.1 AI 美食 Agent（工具调用 + SSE流式）
     *
     * @param message
     * @param chatId
     * @param longitude 用户当前位置经度
     * @param latitude 用户当前位置纬度
     * @return
     */
    public Flux<String> doChatWithFoodAgentStream(String message, String chatId, String longitude, String latitude) {
        String conversationId = normalizeConversationId(chatId);
        rememberUserMessage(conversationId, message);
        String systemPrompt = buildToolSystemPrompt(conversationId, longitude, latitude);
        return streamAndRemember(conversationId, chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(foodTools)
                .stream()
                .content());
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

    private String buildRagSystemPrompt(String conversationId) {
        return buildSystemPromptWithMemory(conversationId) + RAG_PROMPT_SUFFIX;
    }

    private String buildToolSystemPrompt(String conversationId, String longitude, String latitude) {
        StringBuilder prompt = new StringBuilder(buildSystemPromptWithMemory(conversationId))
                .append(TOOL_PROMPT_SUFFIX)
                .append("\n【用户当前位置】\n");
        if (hasCoordinate(longitude, latitude)) {
            prompt.append("- longitude: ").append(longitude).append('\n')
                    .append("- latitude: ").append(latitude).append('\n');
        } else {
            prompt.append("- 当前请求没有提供 longitude/latitude。\n");
        }
        return prompt.toString();
    }

    private boolean hasCoordinate(String longitude, String latitude) {
        return longitude != null && !longitude.isBlank()
                && latitude != null && !latitude.isBlank();
    }

    private QueryTransformer buildRagQueryTransformer() {
        return query -> {
            String rewritten = queryRewriter.doQueryRewrite(query.text());
            String expanded = queryExpansionService.expandForRag(rewritten);
            String finalQuery = (expanded == null || expanded.isBlank() || expanded.equals(rewritten))
                    ? rewritten : expanded;
            return new Query(finalQuery);
        };
    }

    private Flux<String> streamAndRemember(String conversationId, Flux<String> contentFlux) {
        StringBuilder assistantContent = new StringBuilder();
        return contentFlux
                .doOnNext(assistantContent::append)
                .doOnComplete(() -> rememberAiMessage(conversationId, assistantContent.toString()))
                .doOnError(error -> log.warn("流式对话未完成，跳过 AI 回复记忆写入: {}", error.getMessage()));
    }

}
