package com.aiblogstudio.model;

import com.aiblogstudio.enums.BlogPlatform;
import com.aiblogstudio.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity representing an AI-generated blog post.
 * Tracks the full lifecycle from topic input through research, writing, and publishing.
 */
@Entity
@Table(name = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The original topic provided by the user
     */
    @Column(length = 500, nullable = false)
    private String topic;

    /**
     * AI-generated blog title
     */
    @Column(length = 500, nullable = false)
    private String title;

    /**
     * Full blog content (1200-1500 words, markdown-formatted)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * SEO meta description (max 160 chars)
     */
    @Column(columnDefinition = "TEXT")
    private String metaDescription;

    /**
     * Comma-separated tags for Medium
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String storyUrl;

    /**
     * Blog Platform
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BlogPlatform platform = BlogPlatform.DEVTO;

    /**
     * Current publishing status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BlogStatus status = BlogStatus.DRAFT;


    /**
     * Timestamp of blog creation
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * The user who created this blog.
     * Nullable for backward compatibility with existing blogs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

}
