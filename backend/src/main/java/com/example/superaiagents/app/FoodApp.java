package com.example.superaiagents.app;


import com.example.superaiagents.advisor.RetrieverFactoryAdvisor;
import com.example.superaiagents.chatmemory.FileBasedChatMemory;
import com.example.superaiagents.memory.MemoryManager;
import com.example.superaiagents.pojo.FoodReport;
import com.example.superaiagents.rag.QueryExpansionService;
import com.example.superaiagents.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

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
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(20)
//                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT_PREFIX)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        // 自定义日志 Advisor，可按需开启
//                        new MyLoggerAdvisor()
                        // 自定义推理增强 Advisor，可按需开启
//                        new ReReadingAdvisor()
                )
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
        String conversationId = (chatId == null || chatId.isEmpty()) ? "default" : chatId;
        String userId = conversationId; // 暂时用 chatId 作为 userId

        // 添加用户消息到短期记忆
        UserMessage userMessage = new UserMessage(message);
        memoryManager.addUserMessage(conversationId, userId, userMessage);

        // 获取增强的上下文
        String enhancedContext = memoryManager.getContextForAI(conversationId, userId);
        String systemPrompt = SYSTEM_PROMPT_PREFIX + "【上下文】\n" + enhancedContext;

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("AI 回答的内容是: \n{}", content);

        // 添加 AI 消息到短期记忆
        org.springframework.ai.chat.messages.AssistantMessage aiMessage =
                new org.springframework.ai.chat.messages.AssistantMessage(content);
        memoryManager.addAiMessage(conversationId, aiMessage);

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
        String conversationId = (chatId == null || chatId.isEmpty()) ? "default" : chatId;
        String userId = conversationId;

        // 添加用户消息到短期记忆
        UserMessage userMessage = new UserMessage(message);
        memoryManager.addUserMessage(conversationId, userId, userMessage);

        // 获取增强的上下文
        String enhancedContext = memoryManager.getContextForAI(conversationId, userId);
        String systemPrompt = SYSTEM_PROMPT_PREFIX + "【上下文】\n" + enhancedContext;

        Flux<String> content = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        return content;
    }

    /**
     * 2. AI 美食报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public FoodReport doChatWithReport(String message, String chatId) {
        FoodReport foodReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT_PREFIX + "每次对话后都要生成美食报告，标题为推荐菜品名称，内容为制作建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(FoodReport.class);
        log.info("foodReport: {}", foodReport);
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
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入查询重写后回答的内容是: \n{}", content);
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
        // 查询扩展（使用 LLM 模式）
        String expandedMessage = queryExpansionService.expand(message, QueryExpansionService.ExpansionMode.LLM);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(expandedMessage)
                // 先配置参数，再添加Advisor（根据SDK支持的链式调用顺序）
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 直接传入Advisor实例
                .advisors(new QuestionAnswerAdvisor(redisVectorStore))
                // 执行调用
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入rag+查询扩展之后的输出 content内容:\n {}", content);
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
        String conversationId = (chatId == null || chatId.isEmpty()) ? "default" : chatId;
        // 查询扩展（使用 LLM 模式）
        String expandedMessage = queryExpansionService.expand(message, QueryExpansionService.ExpansionMode.LLM);
        return chatClient
                .prompt()
                .user(expandedMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(new QuestionAnswerAdvisor(redisVectorStore))
                .stream()
                .content();
    }


    /**
     * 4.2 和 RAG 知识库进行对话，并且在对话之前使用 检索器配置
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRagWithRetriever(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(RetrieverFactoryAdvisor.createFoodAppRagCustomAdvisor(
                        redisVectorStore, "美食"
                ))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("加入检索器后的 content内容:\n {}", content);
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
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("工具调用的测试 content: \n{}", content);
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
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

}