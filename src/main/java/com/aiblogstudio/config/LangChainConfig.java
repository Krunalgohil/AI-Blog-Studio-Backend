package com.aiblogstudio.config;

import com.aiblogstudio.agents.*;
import com.aiblogstudio.tools.DevToPublisherTool;
import com.aiblogstudio.tools.TavilySearchTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class LangChainConfig {

    // Common timeout for all models
    private static final Duration MODEL_TIMEOUT = Duration.ofSeconds(120); // 3 minutes

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    @Value("${model.api-key}")
    private String apiKey;

    @Value("${model.base-url}")
    private String baseUrl;

    @Value("${model.name}")
    private String modelName;

    @Value("${model.rpm-limit:50}")
    private int rpmLimit;

    // ThreadLocal context
    private static final ThreadLocal<String> CURRENT_STAGE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<String> DYNAMIC_MODEL_KEY = new ThreadLocal<>();
    private static final ThreadLocal<String> DYNAMIC_PROVIDER_KEY = new ThreadLocal<>();

    public static void setCurrentStage(String stage) {
        CURRENT_STAGE.set(stage);
    }

    public static String getCurrentStage() {
        return CURRENT_STAGE.get();
    }

    public static void setCurrentModel(String model) {
        CURRENT_MODEL.set(model);
    }

    public static String getCurrentModel() {
        return CURRENT_MODEL.get();
    }

    public static void setDynamicModelKey(String modelKey) {
        DYNAMIC_MODEL_KEY.set(modelKey);
    }

    public static String getDynamicModelKey() {
        return DYNAMIC_MODEL_KEY.get();
    }

    public static void setDynamicProviderKey(String providerKey) {
        DYNAMIC_PROVIDER_KEY.set(providerKey);
    }

    public static String getDynamicProviderKey() {
        return DYNAMIC_PROVIDER_KEY.get();
    }

    public static void clearContext() {
        CURRENT_STAGE.remove();
        CURRENT_MODEL.remove();
        DYNAMIC_MODEL_KEY.remove();
        DYNAMIC_PROVIDER_KEY.remove();
    }

    @Bean
    public TavilyWebSearchEngine tavilyWebSearchEngine() {
        return TavilyWebSearchEngine.builder()
                .apiKey(tavilyApiKey)
                .includeAnswer(true)
                .includeRawContent(false)
                .build();
    }

    // ========== DEFAULT CONFIGURABLE MODEL (REQUIRED) ==========
    @Bean
    public ChatModel defaultModel() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("❌ Model API key is missing! ChatModel cannot be created.");
        }
        log.info("✅ Creating ChatModel with name: {}, base URL: {}", modelName, baseUrl);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.4)
                .timeout(MODEL_TIMEOUT)
                .build();
    }

    // Rate Limiters
    @Bean
    public RateLimiter defaultRateLimiter() {
        return RateLimiter.of("default", buildRateLimiterConfig(rpmLimit));
    }

    private RateLimiterConfig buildRateLimiterConfig(int rpm) {
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(rpm)
                .timeoutDuration(Duration.ofSeconds(180))
                .build();
    }

    // Model Registry
    @Bean
    @Qualifier("customModelRegistry")
    public Map<String, ChatModel> modelRegistry(ChatModel defaultModel) {
        Map<String, ChatModel> registry = new HashMap<>();
        registry.put("default", defaultModel);
        registry.put("step-flash", defaultModel);
        registry.put("glm", defaultModel);

        log.info("📦 Custom model registry initialized with default model key: {}", modelName);
        return registry;
    }

    // Provider Rate Limiter Registry
    @Bean
    @Qualifier("customRateLimiterRegistry")
    public Map<String, RateLimiter> providerRateLimiterRegistry(RateLimiter defaultRateLimiter) {
        Map<String, RateLimiter> registry = new HashMap<>();
        registry.put("default", defaultRateLimiter);
        registry.put("stepfun", defaultRateLimiter);
        registry.put("openrouter", defaultRateLimiter);
        log.info("🚦 Custom rate limiter registry initialized with default rate limiter.");
        return registry;
    }

    private ChatModel resolveModel(Map<String, ChatModel> registry, String preferredKey, String agentName) {
        ChatModel model = registry.get("default");
        if (model == null) {
            throw new IllegalStateException("❌ Default ChatModel is missing from registry!");
        }
        log.info("🎯 {} using default model: {}", agentName, modelName);
        return model;
    }

    // ========== AGENTS ==========
    @Bean
    public PlannerAgent plannerAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry) {
        return AiServices.builder(PlannerAgent.class)
                .chatModel(resolveModel(registry, "default", "PlannerAgent"))
                .build();
    }

    @Bean
    public ResearcherAgent researcherAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry,
                                           TavilyWebSearchEngine tavilyWebSearchEngine) {
        return AiServices.builder(ResearcherAgent.class)
                .chatModel(resolveModel(registry, "default", "ResearcherAgent"))
                .tools(new TavilySearchTool(tavilyWebSearchEngine))
                .build();
    }

    @Bean
    public WriterAgent writerAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry) {
        return AiServices.builder(WriterAgent.class)
                .chatModel(resolveModel(registry, "default", "WriterAgent"))
                .build();
    }

    @Bean
    public EditorAgent editorAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry) {
        return AiServices.builder(EditorAgent.class)
                .chatModel(resolveModel(registry, "default", "EditorAgent"))
                .build();
    }

    @Bean
    public ReflectionAgent reflectionAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry) {
        return AiServices.builder(ReflectionAgent.class)
                .chatModel(resolveModel(registry, "default", "ReflectionAgent"))
                .build();
    }

    @Bean
    public SeoAgent seoAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry) {
        return AiServices.builder(SeoAgent.class)
                .chatModel(resolveModel(registry, "default", "SeoAgent"))
                .build();
    }

    @Bean
    public PublisherAgent publisherAgent(@Qualifier("customModelRegistry") Map<String, ChatModel> registry,
                                         DevToPublisherTool devToPublisherTool) {
        return AiServices.builder(PublisherAgent.class)
                .chatModel(resolveModel(registry, "default", "PublisherAgent"))
                .tools(devToPublisherTool)
                .build();
    }
}