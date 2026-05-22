package com.aiblogstudio.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j tool that publishes articles to Dev.to.
 * Called by the PublisherAgent when it decides to push content live.
 */
@Slf4j
@Component
public class DevToPublisherTool {

    @Value("${devto.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool("publishToDevTo")
    public String publishToDevTo(
            @P("Title of the article") String title,
            @P("Markdown content of the article") String content,
            @P("Comma separated tags") String tags) {

        log.info("🚀 Publishing to Dev.to: '{}'", title);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SpringBootApp/1.0");

            // Dev.to tags must be lowercase, alphanumeric, 2-20 chars, max 4 per article
            List<String> validTags = getValidTags(tags);

            String[] finalTags = validTags.stream()
                    .limit(4)  // Dev.to allows up to 4 tags
                    .toArray(String[]::new);

            Map<String, Object> article = new HashMap<>();
            article.put("title", title);
            article.put("body_markdown", content);
            article.put("published", false);  // Save as draft first, can publish later manually
            article.put("tags", finalTags);

            Map<String, Object> payload = new HashMap<>();
            payload.put("article", article);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://dev.to/api/articles", entity, String.class);

            log.info("📡 Dev.to Response: {}", response.getStatusCode());
            return "✅ SUCCESS: Blog published. Status: " + response.getStatusCode();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ Dev.to API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "FAILED: " + e.getStatusCode() + " " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }

    @NotNull
    private static List<String> getValidTags(String tags) {
        String[] allTags = tags.replaceAll("#", "").split(",\\s*");
        List<String> validTags = new ArrayList<>();

        for (String tag : allTags) {
            String cleanTag = tag.toLowerCase().trim()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "");
            cleanTag = cleanTag.toLowerCase();
            if (cleanTag.length() >= 2 && cleanTag.length() <= 20) {
                validTags.add(cleanTag);
            }
        }
        return validTags;
    }
}