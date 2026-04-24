package com.example.superaiagents.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.retry.support.RetryTemplate;

import io.micrometer.observation.ObservationRegistry;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * ChatClient 配置 - 将 ChatClient 暴露为 Spring Bean
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    private static final Duration AI_HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration AI_HTTP_READ_TIMEOUT = Duration.ofMinutes(2);

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(OpenAiConnectionProperties commonProperties,
                                           OpenAiChatProperties chatProperties,
                                           ToolCallingManager toolCallingManager,
                                           RetryTemplate retryTemplate,
                                           ResponseErrorHandler responseErrorHandler,
                                           ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<ChatModelObservationConvention> observationConvention,
                                           ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(firstText(chatProperties.getBaseUrl(), commonProperties.getBaseUrl()))
                .apiKey(new SimpleApiKey(firstText(chatProperties.getApiKey(), commonProperties.getApiKey())))
                .headers(buildHeaders(chatProperties, commonProperties))
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(createRestClientBuilder())
                .webClientBuilder(createWebClientBuilder())
                .responseErrorHandler(responseErrorHandler)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(toolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        log.info("主对话模型: {}", chatProperties.getOptions().getModel());
        return chatModel;
    }

    @Bean("sidecarChatModel")
    public OpenAiChatModel sidecarChatModel(OpenAiConnectionProperties commonProperties,
                                            OpenAiChatProperties chatProperties,
                                            ToolCallingManager toolCallingManager,
                                            RetryTemplate retryTemplate,
                                            ResponseErrorHandler responseErrorHandler,
                                            ObjectProvider<ObservationRegistry> observationRegistry,
                                            ObjectProvider<ChatModelObservationConvention> observationConvention,
                                            ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate,
                                            @Value("${spring.ai.openai.chat.sidecar.options.model}") String sidecarModel) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(firstText(chatProperties.getBaseUrl(), commonProperties.getBaseUrl()))
                .apiKey(new SimpleApiKey(firstText(chatProperties.getApiKey(), commonProperties.getApiKey())))
                .headers(buildHeaders(chatProperties, commonProperties))
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(createRestClientBuilder())
                .webClientBuilder(createWebClientBuilder())
                .responseErrorHandler(responseErrorHandler)
                .build();

        OpenAiChatOptions sidecarOptions = OpenAiChatOptions.fromOptions(chatProperties.getOptions());
        sidecarOptions.setModel(sidecarModel);

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(sidecarOptions)
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(toolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        log.info("旁路小模型: {}", sidecarOptions.getModel());
        return chatModel;
    }

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return createRestClientBuilder();
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return createWebClientBuilder();
    }

    private RestClient.Builder createRestClientBuilder() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(createAiHttpClient());
        requestFactory.setReadTimeout(AI_HTTP_READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private WebClient.Builder createWebClientBuilder() {
        JdkClientHttpConnector connector = new JdkClientHttpConnector(createAiHttpClient());
        connector.setReadTimeout(AI_HTTP_READ_TIMEOUT);
        return WebClient.builder().clientConnector(connector);
    }

    private HttpClient createAiHttpClient() {
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[]{"TLSv1.2"});
        return HttpClient.newBuilder()
                .connectTimeout(AI_HTTP_CONNECT_TIMEOUT)
                .sslParameters(sslParameters)
                .build();
    }

    private MultiValueMap<String, String> buildHeaders(OpenAiChatProperties chatProperties,
                                                       OpenAiConnectionProperties commonProperties) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String organizationId = firstText(chatProperties.getOrganizationId(), commonProperties.getOrganizationId());
        if (StringUtils.hasText(organizationId)) {
            headers.add("OpenAI-Organization", organizationId);
        }
        String projectId = firstText(chatProperties.getProjectId(), commonProperties.getProjectId());
        if (StringUtils.hasText(projectId)) {
            headers.add("OpenAI-Project", projectId);
        }
        return headers;
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }
}
