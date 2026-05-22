package com.aiblogstudio.dto;

import com.aiblogstudio.enums.BlogPlatform;
import com.aiblogstudio.enums.BlogStatus;
import com.aiblogstudio.model.Blog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for sending blog details to the frontend.
 * Excludes internal fields (like raw AI responses) and flattens the Blog entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogDetailDto {
    private Long id;
    private String topic;
    private String title;
    private String content;
    private String summary;
    private String metaDescription;
    private String tags;
    private String storyUrl;
    private BlogPlatform platform;
    private BlogStatus status;
    private LocalDateTime createdAt;
    private String createdByEmail;
    private String createdByName;

    /**
     * Converts a Blog entity to a BlogDetailDto.
     * Handles null safely to avoid API crashes.
     */
    public static BlogDetailDto fromEntity(Blog blog) {
        if (blog == null) return null;
        return BlogDetailDto.builder()
                .id(blog.getId())
                .topic(blog.getTopic())
                .title(blog.getTitle())
                .content(blog.getContent())
                .summary(blog.getSummary())
                .metaDescription(blog.getMetaDescription())
                .tags(blog.getTags())
                .storyUrl(blog.getStoryUrl())
                .platform(blog.getPlatform())
                .status(blog.getStatus())
                .createdAt(blog.getCreatedAt())
                .createdByEmail(blog.getCreatedBy() != null ? blog.getCreatedBy().getEmail() : null)
                .createdByName(blog.getCreatedBy() != null ? blog.getCreatedBy().getFullName() : null)
                .build();
    }
}