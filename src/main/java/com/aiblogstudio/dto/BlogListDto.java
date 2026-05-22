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
 * Lightweight DTO for blog list views.
 * Excludes heavy fields like full content and meta description.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogListDto {
    private Long id;
    private String topic;
    private String title;
    private String tags;
    private BlogPlatform platform;
    private BlogStatus status;
    private LocalDateTime createdAt;

    /**
     * Maps a Blog entity to a list-friendly DTO.
     * Returns null if input is null.
     */
    public static BlogListDto fromEntity(Blog blog) {
        if (blog == null) return null;
        return BlogListDto.builder()
                .id(blog.getId())
                .topic(blog.getTopic())
                .title(blog.getTitle())
                .tags(blog.getTags())
                .platform(blog.getPlatform())
                .status(blog.getStatus())
                .createdAt(blog.getCreatedAt())
                .build();
    }
}