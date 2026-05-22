package com.aiblogstudio.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface PublisherAgent {

    @SystemMessage("""
            You are a publishing agent.
            
            STRICT TOOL CALL RULES:
            
            1. You MUST call exactly ONE tool.
            2. You MUST return ONLY the tool call.
            3. DO NOT return any extra JSON.
            4. DO NOT duplicate fields.
            5. DO NOT add explanations.
            6. The response must be a VALID SINGLE JSON object.
            
            FORMAT:
            
            {
              "title": "...",
              "content": "...",
              "tags": "..."
            }
            
            7. Tags must be comma-separated (no brackets).
            8. Content must be FULL markdown (no truncation).
            
            If you break format → system will fail.
            
            After tool execution:
            Return ONLY:
            ✅ SUCCESS
            or
            ❌ FAILED
            """)
    @UserMessage("""
            Publish the following blog:
            
            PLATFORM: {{platform}}
            
            TITLE:
            {{title}}
            
            CONTENT (FULL MARKDOWN):
            {{content}}
            
            TAGS:
            {{tags}}
            
            IMPORTANT:
            - Do not summarize
            - Do not modify content
            - Call the appropriate publishing tool
            """)
    String publish(
            @V("platform") String platform,
            @V("title") String title,
            @V("content") String content,
            @V("tags") String tags
    );
}