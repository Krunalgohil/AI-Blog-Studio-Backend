package com.aiblogstudio.service;

import com.aiblogstudio.agents.*;
import com.aiblogstudio.config.LangChainConfig;
import com.aiblogstudio.dto.BlogDetailDto;
import com.aiblogstudio.dto.BlogResponse;
import com.aiblogstudio.enums.BlogStatus;
import com.aiblogstudio.model.Blog;
import com.aiblogstudio.model.User;
import com.aiblogstudio.repository.BlogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class BlogOrchestratorService {

    private final PlannerAgent plannerAgent;
    private final ResearcherAgent researcherAgent;
    private final WriterAgent writerAgent;
    private final EditorAgent editorAgent;
    private final ReflectionAgent reflectionAgent;
    private final SeoAgent seoAgent;
    private final PublisherAgent publisherAgent;
    private final BlogRepository blogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("customModelRegistry")
    private final Map<String, ChatModel> modelRegistry;

    @Qualifier("customRateLimiterRegistry")
    private final Map<String, RateLimiter> providerRateLimiterRegistry;

    @Value("${auto-publish}")
    private boolean autoPublish;

    public BlogOrchestratorService(
            PlannerAgent plannerAgent,
            ResearcherAgent researcherAgent,
            WriterAgent writerAgent,
            EditorAgent editorAgent,
            ReflectionAgent reflectionAgent,
            SeoAgent seoAgent,
            PublisherAgent publisherAgent,
            BlogRepository blogRepository,
            @Qualifier("customModelRegistry") Map<String, dev.langchain4j.model.chat.ChatModel> modelRegistry,
            @Qualifier("customRateLimiterRegistry") Map<String, RateLimiter> providerRateLimiterRegistry) {
        this.plannerAgent = plannerAgent;
        this.researcherAgent = researcherAgent;
        this.writerAgent = writerAgent;
        this.editorAgent = editorAgent;
        this.reflectionAgent = reflectionAgent;
        this.seoAgent = seoAgent;
        this.publisherAgent = publisherAgent;
        this.blogRepository = blogRepository;
        this.modelRegistry = modelRegistry;
        this.providerRateLimiterRegistry = providerRateLimiterRegistry;
    }

    /**
     * Backward-compatible overload without user.
     */
    public BlogResponse generateAndPublish(String topic, String platform) {
        return generateAndPublish(topic, platform, null);
    }

    /**
     * Generate and publish a blog, associating it with the specified user.
     */
    public BlogResponse generateAndPublish(String topic, String platform, User user) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🚀 Starting autonomous blog pipeline for topic: '{}'", topic);
        log.info("📌 Target platform: '{}'", platform);
        log.info("═══════════════════════════════════════════════════════════");



        try {
            String finalTopic = topic;
            log.info("📝 Step 1: Planner Agent is crafting the blog outline...");
            String plan = executeWithRateLimit("glm", "planner", () -> plannerAgent.plan(finalTopic));
            log.info("✅ Step 1 Complete: Outline generated.");

            log.info("🔍 Step 2: Researcher Agent is gathering factual data...");
            String researchInput = String.format("USER TOPIC:\n%s\n\nPLANNER OUTPUT:\n%s", topic, plan);
            String researchNotes = executeWithRateLimit("glm", "research", () -> researcherAgent.research(researchInput));
            log.info("✅ Step 2 Complete: Research notes compiled.");

            log.info("✍️ Step 3: Writer Agent is drafting the initial blog...");
            String rawBlog = executeWithRateLimit("step-flash", "write", () -> writerAgent.writeBlog("PLANNER OUTPUT:\n" + plan, researchNotes));
            log.info("✅ Step 3 Complete: Initial draft written.");

            log.info("🎨 Step 4: Editor Agent is refining and humanizing the content...");
            String editedBlog = executeWithRateLimit("glm", "edit", () -> editorAgent.editAndHumanize(rawBlog, researchNotes, ""));
            log.info("✅ Step 4 Complete: Blog polished.");

            log.info("🧠 Step 5: Reflection Agent is reviewing the content for quality...");
            String reflectedBlog = runReflectionLoop(editedBlog, researchNotes, 2);
            log.info("✅ Step 5 Complete: Self-reflection and improvements applied.");

            log.info("📈 Step 6: SEO Agent is optimizing meta tags and keywords...");
            String finalContentWithMeta = executeWithRateLimit("glm", "seo", () -> seoAgent.optimizeForSeo(reflectedBlog));
            log.info("✅ Step 6 Complete: SEO optimization finished.");

            String title = extractTitle(finalContentWithMeta);
            String metaDescription = extractMetaDescription(finalContentWithMeta);
            String tags = extractTags(finalContentWithMeta);
            String cleanContent = cleanContent(finalContentWithMeta);

            title = truncate(title, 200);
            topic = truncate(topic, 200);
            tags = truncate(tags, 400);
            metaDescription = truncate(metaDescription, 500);

            Blog blog = Blog.builder()
                    .topic(topic).title(title).content(cleanContent)
                    .metaDescription(metaDescription).tags(tags).status(BlogStatus.DRAFT)
                    .createdBy(user).build();

            String publishResult = "Skipped — auto-publish is disabled";
            if (autoPublish) {
                try {
                    String finalPlatform = normalizePlatform(platform);
                    if ("medium".equals(finalPlatform)) {
                        log.warn("⚠️ Medium publishing is disabled because Playwright integration was removed.");
                        publishResult = "Medium publishing is disabled (Playwright removed). Saved as DRAFT locally.";
                        blog.setStatus(BlogStatus.DRAFT);
                    } else {
                        String finalTitle = title;
                        String finalTags = tags;
                        publishResult = executeWithRateLimit("default", "publish",
                                () -> publisherAgent.publish(finalPlatform, finalTitle, cleanContent, finalTags));
                        boolean success = publishResult != null && publishResult.contains("✅ SUCCESS") && !publishResult.contains("❌ FAILED");
                        blog.setStatus(success ? BlogStatus.PUBLISHED : BlogStatus.DRAFT);
                    }
                } catch (Exception e) {
                    log.error("❌ Publishing failed: {}", e.getMessage(), e);
                    publishResult = "Publishing failed: " + e.getMessage();
                    blog.setStatus(BlogStatus.DRAFT);
                }
            }

            blog = blogRepository.save(blog);
            log.info("💾 Blog saved with ID: {}", blog.getId());



            log.info("═══════════════════════════════════════════════════════════");
            log.info("🎉 Pipeline complete! Blog: '{}' [{}]", title, blog.getStatus());
            log.info("═══════════════════════════════════════════════════════════");

            return BlogResponse.builder().success(true)
                    .message(String.format("Blog '%s' generated successfully! Status: %s. %s", title, blog.getStatus(), publishResult))
                    .blog(BlogDetailDto.fromEntity(blog)).build();

        } catch (Exception e) {
            log.error("❌ Pipeline failed for topic '{}': {}", topic, e.getMessage(), e);

            return BlogResponse.builder().success(false).message("Pipeline failed: " + e.getMessage()).blog(null).build();
        }
    }

    private <T> T executeWithRateLimit(String preferredModelKey, String stage, Supplier<T> action) {
        // Determine actual model and its registry key
        dev.langchain4j.model.chat.ChatModel actualModel = modelRegistry.get("default");
        String actualModelKey = "default";

        String providerKey = getProviderForModelKey(actualModelKey);
        RateLimiter limiter = providerRateLimiterRegistry.get(providerKey);

        // Debug log to verify correct keys
        log.debug("Stage '{}' using modelKey '{}', providerKey '{}', limiter present: {}",
                stage, actualModelKey, providerKey, limiter != null);

        // Set ThreadLocal for token metrics
        LangChainConfig.setCurrentStage(stage);
        LangChainConfig.setCurrentModel(actualModelKey);

        Supplier<T> call = () -> {
            try {
                return action.get();
            } finally {
                LangChainConfig.clearContext();
            }
        };

        if (limiter == null) {
            log.warn("No rate limiter for provider '{}' (stage '{}'). Proceeding without limit.",
                    providerKey, stage);
            return call.get();
        }

        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return RateLimiter.decorateSupplier(limiter, call).get();
            } catch (RequestNotPermitted e) {
                long waitMs = parseWaitTime(e.getMessage());
                log.warn("⏳ Rate limit hit for provider '{}' (stage '{}'). Waiting {}ms (attempt {}/{})",
                        providerKey, stage, waitMs, attempt, maxRetries);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("Rate limit retries exhausted for stage: " + stage);
    }

    private String getProviderForModelKey(String modelKey) {
        return "default";
    }

    private String runReflectionLoop(String blogContent, String researchNotes, int maxRetries) {
        String current = blogContent;
        for (int i = 1; i <= maxRetries; i++) {

            String finalCurrent1 = current;
            String result = executeWithRateLimit("glm", "reflection", () -> reflectionAgent.reflect(finalCurrent1, researchNotes));
            if (isReflectionPass(result) && extractReflectionScore(result) >= 8) return current;
            String fixes = extractReflectionFixes(result);
            String finalCurrent = current;
            current = executeWithRateLimit("glm", "edit", () -> editorAgent.editAndHumanize(finalCurrent, researchNotes, fixes));
        }
        return current;
    }

    private long parseWaitTime(String errorMessage) {
        try {
            if (errorMessage != null && errorMessage.contains("try again in")) {
                String secs = errorMessage.replaceAll(".*try again in ([0-9.]+).*", "$1");
                return (long) (Double.parseDouble(secs) * 1000) + 2000;
            }
        } catch (Exception ignored) {
        }
        return 20_000;
    }

    // --- Helper methods (unchanged) ---
    private boolean isReflectionPass(String json) {
        try {
            return objectMapper.readTree(json).path("pass").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private int extractReflectionScore(String json) {
        try {
            return objectMapper.readTree(json).path("score").asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractReflectionFixes(String json) {
        try {
            return objectMapper.readTree(json).path("fixes").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizePlatform(String p) {
        if (p == null || p.isBlank()) return "devto";
        String norm = p.trim().toLowerCase();
        return norm.equals("dev.to") || norm.equals("devto") ? "devto" : norm.equals("medium") ? "medium" : norm;
    }

    private String extractTitle(String content) {
        for (String line : content.split("\n")) {
            String t = line.trim();
            String upper = t.toUpperCase();
            if (!t.isEmpty() && !upper.startsWith("META_DESCRIPTION") && !upper.startsWith("TAGS") && !t.equalsIgnoreCase("[Full Blog]"))
                return t.replaceAll("[#*']", "").trim();
        }
        return "Untitled Blog";
    }

    private String extractMetaDescription(String content) {
        for (String line : content.split("\n")) {
            if (line.toLowerCase().contains("meta description") || line.toLowerCase().contains("meta_description")) {
                int idx = line.indexOf(":");
                if (idx != -1) return line.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private String extractTags(String content) {
        for (String line : content.split("\n")) {
            if (line.toLowerCase().startsWith("tags")) {
                int idx = line.indexOf(":");
                if (idx != -1) return line.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private String cleanContent(String content) {
        StringBuilder sb = new StringBuilder();
        for (String line : content.split("\n")) {
            String l = line.toLowerCase().trim();
            if (l.contains("meta description") || l.contains("meta_description") || l.startsWith("tags") || l.contains("word count") || l.equals("[full blog]")) continue;
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    private int safeLength(String s) {
        return s == null ? 0 : s.length();
    }

    private String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}