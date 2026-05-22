package com.aiblogstudio.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Tool for researching topics using Tavily Web Search API.
 * Called by the ResearcherAgent to fetch up-to-date information.
 */
@Slf4j
@Component
public class TavilySearchTool {

    private final TavilyWebSearchEngine searchEngine;

    public TavilySearchTool(TavilyWebSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @Tool("Search the web for the latest information, statistics, news, and data on any topic. " +
            "Use this to gather research material for writing blog posts. " +
            "Returns titles, content snippets, and source URLs.")
    public String searchWeb(String query) {
        log.info("🔍 tavily search: '{}'", query);
        try {
            WebSearchResults results = searchEngine.search(query);

            // Format each result as: title, snippet, source URL
            String formattedResults = results.results().stream()
                    .map(result -> String.format(
                            "**%s**\n%s\nSource: %s\n",
                            result.title(),
                            result.snippet() != null ? result.snippet() : "(no snippet)",
                            result.url()
                    ))
                    .collect(Collectors.joining("\n---\n"));

            log.info("✅ Found {} results for '{}'", results.results().size(), query);
            return formattedResults.isEmpty()
                    ? "No results found for: " + query
                    : formattedResults;

        } catch (Exception e) {
            log.error("❌ tavily search failed for '{}': {}", query, e.getMessage());
            return "Search failed: " + e.getMessage() + ". Please proceed with general knowledge.";
        }
    }
}