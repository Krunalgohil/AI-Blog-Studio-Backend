package com.aiblogstudio.repository;

import com.aiblogstudio.dto.BlogListDto;
import com.aiblogstudio.model.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Blog entities.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    /** Retrieve all blogs ordered by most recent first */
    List<Blog> findAllByOrderByCreatedAtDesc();

    /** 
     * Highly optimized query that fetches only the absolute necessary fields for the list view,
     * avoiding loading the heavy Markdown 'content' string from the database.
     */
    @Query("""
            SELECT new com.aiblogstudio.dto.BlogListDto(
                b.id,
                b.topic,
                b.title,
                b.tags,
                b.platform,
                b.status,
                b.createdAt
            )
            FROM Blog b
            ORDER BY b.createdAt DESC
            """)
    List<BlogListDto> summarizeAllBlogs();

    /** Fetch blogs created by a specific user */
    List<Blog> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    /** Lightweight list of blogs for a specific user */
    @Query("""
            SELECT new com.aiblogstudio.dto.BlogListDto(
                b.id,
                b.topic,
                b.title,
                b.tags,
                b.platform,
                b.status,
                b.createdAt
            )
            FROM Blog b
            WHERE b.createdBy.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<BlogListDto> summarizeBlogsByUser(@Param("userId") Long userId);
}

