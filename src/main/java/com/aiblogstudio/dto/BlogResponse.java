package com.aiblogstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned by the blog generation endpoint.
 * Carries the result status, a human-readable message, and the generated blog.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogResponse {

    /**
     * Whether the generation/publishing pipeline succeeded
     */
    private boolean success;

    /**
     * Human-readable status message (e.g., "Blog saved as draft on Platform")
     */
    private String message;

    /**
     * The generated Blog DTO (null if pipeline failed)
     */
    private BlogDetailDto blog;
}
